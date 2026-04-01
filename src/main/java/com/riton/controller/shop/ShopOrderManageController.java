package com.riton.controller.shop;

import com.riton.domain.dto.Result;
import com.riton.domain.dto.ShopAccountDTO;
import com.riton.service.IVoucherOrderService;
import com.riton.utils.ShopHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 店铺订单管理控制器。
 */
@RestController
@RequestMapping("/shop/manage/orders")
public class ShopOrderManageController {

    private final IVoucherOrderService voucherOrderService;

    /**
     * 构造店铺订单管理控制器。
     *
     * @param voucherOrderService 优惠券订单服务
     */
    @Autowired
    public ShopOrderManageController(IVoucherOrderService voucherOrderService) {
        this.voucherOrderService = voucherOrderService;
    }

    /**
     * 分页查询当前店铺订单。
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @param status 订单状态
     * @return 订单列表
     */
    @GetMapping
    public Result queryOrders(@RequestParam(value = "page", defaultValue = "1") Integer page,
                              @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                              @RequestParam(value = "status", required = false) Integer status) {
        // 第一步：读取登录店铺ID。
        ShopAccountDTO shopAccountDTO = ShopHolder.getShop();
        if (shopAccountDTO == null || shopAccountDTO.getShopId() == null) {
            return Result.fail("请先登录店铺账号");
        }

        // 第二步：按店铺分页查询订单。
        return voucherOrderService.queryShopOrders(shopAccountDTO.getShopId(), page, pageSize, status);
    }

    /**
     * 核销订单（仅PAID->FINISHED）。
     *
     * @param orderId 订单ID
     * @return 处理结果
     */
    @PutMapping("/{orderId}/verify")
    public Result verifyOrder(@PathVariable("orderId") Long orderId) {
        // 第一步：读取登录店铺ID。
        ShopAccountDTO shopAccountDTO = ShopHolder.getShop();
        if (shopAccountDTO == null || shopAccountDTO.getShopId() == null) {
            return Result.fail("请先登录店铺账号");
        }

        // 第二步：执行核销状态流转。
        return voucherOrderService.verifyShopOrder(shopAccountDTO.getShopId(), orderId);
    }

    /**
     * 完成退款（仅REFUNDING->REFUNDED）。
     *
     * @param orderId 订单ID
     * @return 处理结果
     */
    @PutMapping("/{orderId}/refund/finish")
    public Result finishRefund(@PathVariable("orderId") Long orderId) {
        // 第一步：读取登录店铺ID。
        ShopAccountDTO shopAccountDTO = ShopHolder.getShop();
        if (shopAccountDTO == null || shopAccountDTO.getShopId() == null) {
            return Result.fail("请先登录店铺账号");
        }

        // 第二步：执行退款完成状态流转。
        return voucherOrderService.finishShopRefund(shopAccountDTO.getShopId(), orderId);
    }
}
