package com.riton.controller;

import com.riton.dto.PaymentDTO;
import com.riton.dto.Result;
import com.riton.service.IPayService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/pay")
public class PayController {

    private final IPayService payService;

    public PayController(IPayService payService) {
        this.payService = payService;
    }

    @PostMapping
    public Result pay(@RequestBody PaymentDTO paymentDTO) {
        return payService.pay(paymentDTO);
    }

    @PostMapping("/callback/alipay")
    public String alipayCallback(@RequestParam Map<String, String> callbackParams) {
        // 支付宝异步回调入口：实际业务处理在 payService.alipayCallback(...)，
        // 内部会调用 onThirdPartyPaySuccess(...) 完成“查状态->改状态/退款”。
        return payService.alipayCallback(callbackParams);
    }

    @PostMapping("/callback/wechat")
    public String wechatCallback(@RequestBody String body,
                                 @RequestHeader("Wechatpay-Nonce") String nonce,
                                 @RequestHeader("Wechatpay-Signature") String signature,
                                 @RequestHeader("Wechatpay-Serial") String serial,
                                 @RequestHeader("Wechatpay-Timestamp") String timestamp) {
        // 微信支付异步回调入口：实际业务处理在 payService.wechatCallback(...)，
        // 内部会调用 onThirdPartyPaySuccess(...) 完成“查状态->改状态/退款”。
        return payService.wechatCallback(body, nonce, signature, serial, timestamp);
    }
}
