package com.riton.service;

import com.riton.domain.dto.Result;
import com.riton.domain.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    /**
     * 下单秒杀券
     * @param voucherId 秒杀券的id
     * @return 如果预下单成功，返回订单id
     */
    Result seckillVoucher(Long voucherId);

    /**
     * 下单普通券
     * @param voucherId 秒杀券的id
     * @return 如果预下单成功，返回订单id
     */
    Result voucher(Long voucherId);

    /**
     * 分页查询店铺订单。
     *
     * @param shopId   店铺 ID
     * @param page     页码
     * @param pageSize 每页数量
     * @param status   订单状态
     * @return 分页结果
     */
    Result queryShopOrders(Long shopId, Integer page, Integer pageSize, Integer status);

    /**
     * 核销店铺订单（PAID -> FINISHED）。
     *
     * @param shopId  店铺 ID
     * @param orderId 订单 ID
     * @return 核销结果
     */
    Result verifyShopOrder(Long shopId, Long orderId);

    /**
     * 完成店铺退款（REFUNDING -> REFUNDED）。
     *
     * @param shopId  店铺 ID
     * @param orderId 订单 ID
     * @return 更新结果
     */
    Result finishShopRefund(Long shopId, Long orderId);
}
