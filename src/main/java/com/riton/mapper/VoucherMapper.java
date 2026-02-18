package com.riton.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.riton.entity.Voucher;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface VoucherMapper extends BaseMapper<Voucher> {

    List<Voucher> queryVoucherOfShop(@Param("shopId") Long shopId);

    List<Long> queryVoucherIdsOfShop(@Param("shopId") Long shopId);

    Voucher queryVoucherById(@Param("voucherId") Long voucherId);

    List<Voucher> queryVoucherByIds(@Param("ids") List<Long> ids);
}
