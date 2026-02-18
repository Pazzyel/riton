package com.riton.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.riton.config.PayProperties;
import com.riton.constants.OrderStatutesConstants;
import com.riton.constants.PayMethodConstants;
import com.riton.constants.RedisConstants;
import com.riton.dto.PaymentDTO;
import com.riton.dto.Result;
import com.riton.entity.User;
import com.riton.entity.Voucher;
import com.riton.entity.VoucherOrder;
import com.riton.mapper.UserMapper;
import com.riton.mapper.VoucherMapper;
import com.riton.mapper.VoucherOrderMapper;
import com.riton.service.IPayService;
import com.riton.utils.UserHolder;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class PayServiceImpl implements IPayService {

    private static final String ALIPAY_SUCCESS = "success";
    private static final String ALIPAY_FAIL = "failure";
    private static final String WECHAT_SUCCESS = "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";
    private static final String WECHAT_FAIL = "{\"code\":\"FAIL\",\"message\":\"失败\"}";

    private final RedissonClient redissonClient;
    private final VoucherOrderMapper voucherOrderMapper;
    private final VoucherMapper voucherMapper;
    private final UserMapper userMapper;
    private final PayProperties payProperties;

    private volatile Config wechatConfig;
    private volatile NotificationParser wechatNotificationParser;
    private volatile NativePayService wechatNativePayService;
    private volatile RefundService wechatRefundService;

    public PayServiceImpl(RedissonClient redissonClient,
                          VoucherOrderMapper voucherOrderMapper,
                          VoucherMapper voucherMapper,
                          UserMapper userMapper,
                          PayProperties payProperties) {
        this.redissonClient = redissonClient;
        this.voucherOrderMapper = voucherOrderMapper;
        this.voucherMapper = voucherMapper;
        this.userMapper = userMapper;
        this.payProperties = payProperties;
    }

    @Override
    public Result pay(PaymentDTO paymentDTO) {
        if (paymentDTO == null || paymentDTO.getOrderId() == null || paymentDTO.getPayMethod() == null) {
            return Result.fail("支付参数不完整");
        }

        Integer payMethod = paymentDTO.getPayMethod().intValue();
        if (!isSupportedPayMethod(payMethod)) {
            return Result.fail("不支持的支付方式");
        }

        Long currentUserId = UserHolder.getUser().getId();
        Long orderId = paymentDTO.getOrderId();

        RLock lock = redissonClient.getLock(RedisConstants.LOCK_ORDER_KEY + orderId);
        boolean locked = lock.tryLock();
        if (!locked) {
            return Result.fail("订单正在处理中，请稍后重试");
        }

        try {
            VoucherOrder order = voucherOrderMapper.selectById(orderId);
            if (order == null) {
                return Result.fail("订单不存在");
            }
            if (!currentUserId.equals(order.getUserId())) {
                return Result.fail("无权操作该订单");
            }
            if (!OrderStatutesConstants.UNPAID.equals(order.getStatus())) {
                return Result.fail("订单状态不是待支付，无法支付");
            }

            Voucher voucher = voucherMapper.selectById(order.getVoucherId());
            if (voucher == null || voucher.getPayValue() == null || voucher.getPayValue() <= 0) {
                return Result.fail("订单金额异常");
            }

            if (PayMethodConstants.BALANCE.equals(payMethod)) {
                return payByBalance(order, voucher.getPayValue());
            }
            if (PayMethodConstants.ALIPAY.equals(payMethod)) {
                return createAlipayOrder(order, voucher.getPayValue());
            }
            return createWechatOrder(order, voucher.getPayValue());
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public String alipayCallback(Map<String, String> callbackParams) {
        if (callbackParams == null || callbackParams.isEmpty()) {
            return ALIPAY_FAIL;
        }
        try {
            // 1) 校验支付宝回调签名
            boolean signValid = AlipaySignature.rsaCheckV1(
                    callbackParams,
                    payProperties.getAlipay().getAlipayPublicKey(),
                    payProperties.getAlipay().getCharset(),
                    payProperties.getAlipay().getSignType()
            );
            if (!signValid) {
                return ALIPAY_FAIL;
            }

            String tradeStatus = callbackParams.get("trade_status");
            if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
                return ALIPAY_FAIL;
            }

            String outTradeNo = callbackParams.get("out_trade_no");
            if (!StringUtils.hasText(outTradeNo)) {
                return ALIPAY_FAIL;
            }
            Long orderId = Long.valueOf(outTradeNo);
            String thirdTradeNo = callbackParams.get("trade_no");
            // 2) 回调核心处理函数：在分布式锁内执行“查订单状态->状态流转/退款”
            boolean success = onThirdPartyPaySuccess(orderId, PayMethodConstants.ALIPAY, thirdTradeNo);
            return success ? ALIPAY_SUCCESS : ALIPAY_FAIL;
        } catch (Exception e) {
            log.error("支付宝回调处理失败", e);
            return ALIPAY_FAIL;
        }
    }

    @Override
    public String wechatCallback(String body, String nonce, String signature, String serial, String timestamp) {
        if (!StringUtils.hasText(body) || !StringUtils.hasText(nonce) || !StringUtils.hasText(signature)
                || !StringUtils.hasText(serial) || !StringUtils.hasText(timestamp)) {
            return WECHAT_FAIL;
        }

        try {
            // 1) 使用微信支付 SDK 解析并验签回调报文
            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(serial)
                    .nonce(nonce)
                    .signature(signature)
                    .timestamp(timestamp)
                    .body(body)
                    .build();
            Transaction transaction = getWechatNotificationParser().parse(requestParam, Transaction.class);
            if (transaction == null || !"SUCCESS".equals(String.valueOf(transaction.getTradeState()))) {
                return WECHAT_FAIL;
            }

            Long orderId = Long.valueOf(transaction.getOutTradeNo());
            String transactionId = transaction.getTransactionId();
            // 2) 回调核心处理函数：在分布式锁内执行“查订单状态->状态流转/退款”
            boolean success = onThirdPartyPaySuccess(orderId, PayMethodConstants.WECHAT_PAY, transactionId);
            return success ? WECHAT_SUCCESS : WECHAT_FAIL;
        } catch (Exception e) {
            log.error("微信支付回调处理失败", e);
            return WECHAT_FAIL;
        }
    }

    private Result payByBalance(VoucherOrder order, Long payAmount) {
        int userUpdated = userMapper.update(
                null,
                new UpdateWrapper<User>()
                        .eq("id", order.getUserId())
                        .ge("money", payAmount)
                        .setSql("money = money - " + payAmount)
        );
        if (userUpdated <= 0) {
            return Result.fail("余额不足");
        }

        LocalDateTime now = LocalDateTime.now();
        int orderUpdated = voucherOrderMapper.update(
                null,
                new UpdateWrapper<VoucherOrder>()
                        .eq("id", order.getId())
                        .eq("status", OrderStatutesConstants.UNPAID)
                        .set("status", OrderStatutesConstants.PAID)
                        .set("pay_type", PayMethodConstants.BALANCE)
                        .set("pay_time", now)
                        .set("update_time", now)
        );
        if (orderUpdated <= 0) {
            userMapper.update(
                    null,
                    new UpdateWrapper<User>()
                            .eq("id", order.getUserId())
                            .setSql("money = money + " + payAmount)
            );
            return Result.fail("订单支付失败，请检查订单状态是否还是“未支付");
        }

        return Result.ok(order.getId());
    }

    private Result createAlipayOrder(VoucherOrder order, Long payAmount) {
        try {
            AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
            request.setNotifyUrl(payProperties.getAlipay().getNotifyUrl());
            request.setBizContent(buildAlipayPrecreateBizContent(order.getId(), payAmount));
            // 这里真正把“创建支付宝支付单”的请求发到支付宝服务器
            AlipayTradePrecreateResponse response = buildAlipayClient().execute(request);
            if (response == null || !response.isSuccess()) {
                String message = response == null ? "response is null" : response.getSubMsg();
                return Result.fail("创建支付宝支付单失败: " + message);
            }

            Map<String, Object> data = new HashMap<>(4);
            data.put("orderId", order.getId());
            data.put("payMethod", PayMethodConstants.ALIPAY);
            data.put("outTradeNo", String.valueOf(order.getId()));
            data.put("qrCode", response.getQrCode());
            return Result.ok(data);
        } catch (Exception e) {
            log.error("调用支付宝支付SDK失败, orderId={}", order.getId(), e);
            return Result.fail("支付宝支付调用失败");
        }
    }

    private Result createWechatOrder(VoucherOrder order, Long payAmount) {
        try {
            PrepayRequest request = new PrepayRequest();
            request.setAppid(payProperties.getWechat().getAppId());
            request.setMchid(payProperties.getWechat().getMchId());
            request.setDescription("Voucher Order " + order.getId());
            request.setOutTradeNo(String.valueOf(order.getId()));
            request.setNotifyUrl(payProperties.getWechat().getNotifyUrl());
            Amount amount = new Amount();
            amount.setTotal(payAmount.intValue());
            request.setAmount(amount);

            // 这里真正把“创建微信支付单”的请求发到微信支付服务器
            PrepayResponse response = getWechatNativePayService().prepay(request);
            if (response == null || !StringUtils.hasText(response.getCodeUrl())) {
                return Result.fail("创建微信支付单失败");
            }

            Map<String, Object> data = new HashMap<>(4);
            data.put("orderId", order.getId());
            data.put("payMethod", PayMethodConstants.WECHAT_PAY);
            data.put("outTradeNo", String.valueOf(order.getId()));
            data.put("codeUrl", response.getCodeUrl());
            return Result.ok(data);
        } catch (Exception e) {
            log.error("调用微信支付SDK失败, orderId={}", order.getId(), e);
            return Result.fail("微信支付调用失败");
        }
    }

    private boolean onThirdPartyPaySuccess(Long orderId, Integer payMethod, String thirdTradeNo) {
        // 回调统一落地函数：
        // 1. 加锁保证“查状态->改状态”原子
        // 2. UNPAID -> PAID
        // 3. 非待支付状态触发退款逻辑
        RLock lock = redissonClient.getLock(RedisConstants.LOCK_ORDER_KEY + orderId);
        boolean locked = lock.tryLock();
        if (!locked) {
            return false;
        }

        try {
            VoucherOrder order = voucherOrderMapper.selectById(orderId);
            if (order == null) {
                return false;
            }

            if (OrderStatutesConstants.UNPAID.equals(order.getStatus())) {
                LocalDateTime now = LocalDateTime.now();
                int updated = voucherOrderMapper.update(
                        null,
                        new UpdateWrapper<VoucherOrder>()
                                .eq("id", orderId)
                                .eq("status", OrderStatutesConstants.UNPAID)
                                .set("status", OrderStatutesConstants.PAID)
                                .set("pay_type", payMethod)
                                .set("pay_time", now)
                                .set("update_time", now)
                );
                return updated > 0;
            }

            if (OrderStatutesConstants.PAID.equals(order.getStatus())) {
                return true;
            }

            log.warn("支付回调时订单状态非待支付，触发退款。orderId={}, status={}, payMethod={}, thirdTradeNo={}",
                    orderId, order.getStatus(), payMethod, thirdTradeNo);
            Voucher voucher = voucherMapper.selectById(order.getVoucherId());
            if (voucher == null || voucher.getPayValue() == null || voucher.getPayValue() <= 0) {
                return false;
            }
            return refund(order, voucher.getPayValue(), payMethod);
        } catch (Exception e) {
            log.error("处理第三方支付回调失败, orderId={}", orderId, e);
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private boolean refund(VoucherOrder order, Long payAmount, Integer payMethod) {
        if (PayMethodConstants.ALIPAY.equals(payMethod)) {
            return refundAlipay(order, payAmount);
        }
        if (PayMethodConstants.WECHAT_PAY.equals(payMethod)) {
            return refundWechat(order, payAmount);
        }
        return true;
    }

    private boolean refundAlipay(VoucherOrder order, Long payAmount) {
        try {
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            request.setBizContent(buildAlipayRefundBizContent(order.getId(), payAmount));
            AlipayTradeRefundResponse response = buildAlipayClient().execute(request);
            if (response == null || !response.isSuccess()) {
                log.error("支付宝退款失败, orderId={}, reason={}", order.getId(), response == null ? "response is null" : response.getSubMsg());
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("支付宝退款调用失败, orderId={}", order.getId(), e);
            return false;
        }
    }

    private boolean refundWechat(VoucherOrder order, Long payAmount) {
        try {
            CreateRequest request = new CreateRequest();
            request.setOutTradeNo(String.valueOf(order.getId()));
            request.setOutRefundNo("refund-" + order.getId() + "-" + System.currentTimeMillis());
            request.setReason(payProperties.getRefundReason());
            request.setNotifyUrl(payProperties.getWechat().getNotifyUrl());

            AmountReq amount = new AmountReq();
            amount.setTotal(payAmount);
            amount.setRefund(payAmount);
            amount.setCurrency("CNY");
            request.setAmount(amount);

            getWechatRefundService().create(request);
            return true;
        } catch (Exception e) {
            log.error("微信退款调用失败, orderId={}", order.getId(), e);
            return false;
        }
    }

    private boolean isSupportedPayMethod(Integer payMethod) {
        return PayMethodConstants.BALANCE.equals(payMethod)
                || PayMethodConstants.ALIPAY.equals(payMethod)
                || PayMethodConstants.WECHAT_PAY.equals(payMethod);
    }

    private AlipayClient buildAlipayClient() {
        AlipayConfig config = new AlipayConfig();
        config.setServerUrl(payProperties.getAlipay().getGatewayUrl());
        config.setAppId(payProperties.getAlipay().getAppId());
        config.setPrivateKey(payProperties.getAlipay().getPrivateKey());
        config.setAlipayPublicKey(payProperties.getAlipay().getAlipayPublicKey());
        config.setCharset(payProperties.getAlipay().getCharset());
        config.setSignType(payProperties.getAlipay().getSignType());
        config.setFormat("json");
        try {
            return new DefaultAlipayClient(config);
        } catch (AlipayApiException e) {
            log.error("创建支付宝客户端失败");
            throw new RuntimeException(e);
        }
    }

    private String buildAlipayPrecreateBizContent(Long orderId, Long payAmount) {
        String amount = toYuan(payAmount);
        return "{\"out_trade_no\":\"" + orderId + "\",\"total_amount\":\"" + amount
                + "\",\"subject\":\"Voucher Order " + orderId + "\"}";
    }

    private String buildAlipayRefundBizContent(Long orderId, Long payAmount) {
        String amount = toYuan(payAmount);
        return "{\"out_trade_no\":\"" + orderId + "\",\"refund_amount\":\"" + amount
                + "\",\"refund_reason\":\"" + payProperties.getRefundReason() + "\"}";
    }

    private String toYuan(Long amountFen) {
        return BigDecimal.valueOf(amountFen).divide(BigDecimal.valueOf(100)).toPlainString();
    }

    private Config getWechatConfig() {
        if (wechatConfig == null) {
            synchronized (this) {
                if (wechatConfig == null) {
                    wechatConfig = new RSAAutoCertificateConfig.Builder()
                            .merchantId(payProperties.getWechat().getMchId())
                            .privateKeyFromPath(payProperties.getWechat().getPrivateKeyPath())
                            .merchantSerialNumber(payProperties.getWechat().getMerchantSerialNumber())
                            .apiV3Key(payProperties.getWechat().getApiV3Key())
                            .build();
                }
            }
        }
        //实现了Config, NotificationConfig接口
        return wechatConfig;
    }

    private NotificationParser getWechatNotificationParser() {
        if (wechatNotificationParser == null) {
            synchronized (this) {
                if (wechatNotificationParser == null) {
                    wechatNotificationParser = new NotificationParser((NotificationConfig) getWechatConfig());
                }
            }
        }
        return wechatNotificationParser;
    }

    private NativePayService getWechatNativePayService() {
        if (wechatNativePayService == null) {
            synchronized (this) {
                if (wechatNativePayService == null) {
                    wechatNativePayService = new NativePayService.Builder()
                            .config(getWechatConfig())
                            .build();
                }
            }
        }
        return wechatNativePayService;
    }

    private RefundService getWechatRefundService() {
        if (wechatRefundService == null) {
            synchronized (this) {
                if (wechatRefundService == null) {
                    wechatRefundService = new RefundService.Builder()
                            .config(getWechatConfig())
                            .build();
                }
            }
        }
        return wechatRefundService;
    }
}
