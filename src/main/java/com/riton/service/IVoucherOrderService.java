package com.riton.service;

import com.riton.dto.Result;
import com.riton.entity.VoucherOrder;
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
}
