package com.riton.service;

import com.riton.domain.dto.Result;
import com.riton.domain.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {

    /**
     * 根据id查询商铺信息
     * @param id id
     * @return
     */
    Result queryById(Long id);

    /**
     * 更新商铺信息
     * @param shop 新的商铺信息
     * @return
     */
    @Transactional
    Result update(Shop shop);

    /**
     * 查询给定种类下的店铺列表（如果有位置信息只查询5000米内的）
     * @param typeId 店铺类型id
     * @param current 当前页数
     * @param x 经度
     * @param y 维度
     * @return 返回的店铺列表
     */
    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);
}
