package com.riton.controller.common;

import com.riton.domain.dto.Result;
import com.riton.service.IVoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 优惠券公共查询控制器。
 */
@RestController
@RequestMapping("/voucher")
public class VoucherCommonController {

    private final IVoucherService voucherService;

    /**
     * 构造优惠券公共查询控制器。
     *
     * @param voucherService 优惠券服务
     */
    @Autowired
    public VoucherCommonController(IVoucherService voucherService) {
        this.voucherService = voucherService;
    }

    /**
     * 查询店铺的优惠券列表。
     *
     * @param shopId 店铺id
     * @return 优惠券列表
     */
    @GetMapping("/list/{shopId}")
    public Result queryVoucherOfShop(@PathVariable("shopId") Long shopId) {
        return voucherService.queryVoucherOfShop(shopId);
    }
}
