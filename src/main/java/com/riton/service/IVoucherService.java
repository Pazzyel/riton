package com.riton.service;

import com.riton.domain.dto.Result;
import com.riton.domain.entity.Voucher;
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

    /**
     * 查询指定店铺可管理的优惠券列表。
     *
     * @param shopId 店铺 ID
     * @return 优惠券列表
     */
    Result queryManageVouchers(Long shopId);

    /**
     * 新增普通券（店铺管理端）。
     *
     * @param shopId   店铺 ID
     * @param voucher  优惠券参数
     * @return 新增结果
     */
    Result addVoucherByShop(Long shopId, Voucher voucher);

    /**
     * 新增秒杀券（店铺管理端）。
     *
     * @param shopId   店铺 ID
     * @param voucher  优惠券参数
     * @return 新增结果
     */
    Result addSeckillVoucherByShop(Long shopId, Voucher voucher);

    /**
     * 更新普通券（店铺管理端）。
     *
     * @param shopId   店铺 ID
     * @param voucher  优惠券参数
     * @return 更新结果
     */
    Result updateVoucherByShop(Long shopId, Voucher voucher);

    /**
     * 更新秒杀券（店铺管理端）。
     *
     * @param shopId   店铺 ID
     * @param voucher  优惠券参数
     * @return 更新结果
     */
    Result updateSeckillVoucherByShop(Long shopId, Voucher voucher);

    /**
     * 删除优惠券（店铺管理端）。
     *
     * @param shopId    店铺 ID
     * @param voucherId 优惠券 ID
     * @return 删除结果
     */
    Result deleteVoucherByShop(Long shopId, Long voucherId);
}
