package com.riton.mapper;

import com.riton.domain.entity.VoucherOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

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

    /**
     * 分页查询指定店铺订单。
     *
     * @param shopId 店铺 ID
     * @param offset 偏移量
     * @param limit  限制条数
     * @param status 订单状态
     * @return 订单列表
     */
    @Select("<script>"
            + "SELECT id,user_id,voucher_id,shop_id,pay_type,status,create_time,pay_time,use_time,refund_time,update_time "
            + "FROM tb_voucher_order WHERE shop_id = #{shopId} "
            + "<if test='status != null'> AND status = #{status} </if> "
            + "ORDER BY create_time DESC LIMIT #{offset},#{limit}"
            + "</script>")
    List<VoucherOrder> queryShopOrders(@Param("shopId") Long shopId,
                                       @Param("offset") Integer offset,
                                       @Param("limit") Integer limit,
                                       @Param("status") Integer status);

    /**
     * 统计指定店铺订单数量。
     *
     * @param shopId 店铺 ID
     * @param status 订单状态
     * @return 订单数量
     */
    @Select("<script>"
            + "SELECT COUNT(1) FROM tb_voucher_order WHERE shop_id = #{shopId} "
            + "<if test='status != null'> AND status = #{status} </if>"
            + "</script>")
    Long countShopOrders(@Param("shopId") Long shopId, @Param("status") Integer status);

    /**
     * 核销店铺订单（PAID -> FINISHED）。
     *
     * @param shopId  店铺 ID
     * @param orderId 订单 ID
     * @return 影响行数
     */
    @Update("UPDATE tb_voucher_order SET status = 3, use_time = NOW() "
            + "WHERE id = #{orderId} AND shop_id = #{shopId} AND status = 2")
    Integer verifyShopOrder(@Param("shopId") Long shopId, @Param("orderId") Long orderId);

    /**
     * 完成店铺退款（REFUNDING -> REFUNDED）。
     *
     * @param shopId  店铺 ID
     * @param orderId 订单 ID
     * @return 影响行数
     */
    @Update("UPDATE tb_voucher_order SET status = 6, refund_time = NOW() "
            + "WHERE id = #{orderId} AND shop_id = #{shopId} AND status = 5")
    Integer finishShopRefund(@Param("shopId") Long shopId, @Param("orderId") Long orderId);
}
