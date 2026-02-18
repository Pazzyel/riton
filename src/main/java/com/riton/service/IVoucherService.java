package com.riton.service;

import com.riton.dto.Result;
import com.riton.entity.Voucher;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IVoucherService extends IService<Voucher> {

    Result queryVoucherOfShop(Long shopId);

    Voucher queryVoucherByIdWithCache(Long voucherId);

    void addSeckillVoucher(Voucher voucher);

    Result updateVoucher(Voucher voucher);

    Result updateSeckillVoucher(Voucher voucher);

    Result deleteVoucher(Long voucherId);
}
