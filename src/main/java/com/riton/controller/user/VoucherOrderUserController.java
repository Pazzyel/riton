package com.riton.controller.user;

import com.riton.annotations.RateLimit;
import com.riton.annotations.RequireTokenCheck;
import com.riton.domain.dto.Result;
import com.riton.enums.RateLimitType;
import com.riton.service.IVoucherOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户优惠券下单控制器。
 */
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderUserController {

    private final IVoucherOrderService voucherOrderService;

    /**
     * 构造用户优惠券下单控制器。
     *
     * @param voucherOrderService 优惠券订单服务
     */
    @Autowired
    public VoucherOrderUserController(IVoucherOrderService voucherOrderService) {
        this.voucherOrderService = voucherOrderService;
    }

    /**
     * 用户下单秒杀券。
     *
     * @param voucherId 秒杀券ID
     * @return 下单结果
     */
    @PostMapping("/seckill/{id}")
    @RateLimit(limitType = RateLimitType.API, rate = 1000)
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.seckillVoucher(voucherId);
    }

    /**
     * 用户下单普通券。
     *
     * @param voucherId 普通券ID
     * @return 下单结果
     */
    @PostMapping("/voucher/{id}")
    @RequireTokenCheck
    public Result voucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.voucher(voucherId);
    }
}
