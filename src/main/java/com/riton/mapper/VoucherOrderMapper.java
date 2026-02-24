package com.riton.mapper;

import com.riton.domain.entity.VoucherOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface VoucherOrderMapper extends BaseMapper<VoucherOrder> {


    // UNPAID = 1 CANCELED = 4
    @Update("UPDATE tb_voucher_order SET status = 4 WHERE id = #{orderId} AND status = 1")
    Long closeOrderByIdIfUnpaid(Long orderId);

    // 你也可以用SELECT FOR UPDATE，这样本次事务中查询会加锁，就可以放行UPDATE
    @Select("SELECT status FROM tb_voucher_order WHERE id = #{orderId}")
    Integer getOrderStatus(Long orderId);
}
