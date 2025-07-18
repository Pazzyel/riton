package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
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
}
