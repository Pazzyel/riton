package com.riton.controller.shop;

import com.riton.domain.dto.Result;
import com.riton.domain.dto.ShopAccountDTO;
import com.riton.domain.entity.Shop;
import com.riton.service.IShopService;
import com.riton.utils.ShopHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 店铺管理控制器。
 */
@RestController
@RequestMapping("/shop/manage")
public class ShopManageController {

    private final IShopService shopService;

    /**
     * 构造店铺管理控制器。
     *
     * @param shopService 店铺服务
     */
    @Autowired
    public ShopManageController(IShopService shopService) {
        this.shopService = shopService;
    }

    /**
     * 维护当前店铺资料。
     *
     * @param profileForm 店铺资料
     * @return 更新结果
     */
    @PutMapping("/profile")
    public Result updateProfile(@RequestBody Shop profileForm) {
        // 第一步：读取当前登录店铺。
        ShopAccountDTO shopAccountDTO = ShopHolder.getShop();
        if (shopAccountDTO == null || shopAccountDTO.getShopId() == null) {
            return Result.fail("请先登录店铺账号");
        }

        // 第二步：仅按 token 中 shopId 进行资料维护。
        return shopService.updateProfile(shopAccountDTO.getShopId(), profileForm);
    }
}
