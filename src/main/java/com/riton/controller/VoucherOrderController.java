package com.riton.controller;


import com.riton.dto.Result;
import com.riton.service.IVoucherOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {

    private IVoucherOrderService voucherOrderService;

    @Autowired
    public VoucherOrderController(IVoucherOrderService voucherOrderService) {
        this.voucherOrderService = voucherOrderService;
    }



    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.seckillVoucher(voucherId);
    }

    @PostMapping("voucher/{id}")
    public Result voucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.voucher(voucherId);
    }
}
