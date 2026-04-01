package com.riton.utils;

import com.riton.domain.dto.ShopAccountDTO;

/**
 * 店铺账号 ThreadLocal 上下文。
 */
public class ShopHolder {

    private static final ThreadLocal<ShopAccountDTO> TL = new ThreadLocal<>();

    /**
     * 保存当前店铺账号。
     *
     * @param shopAccountDTO 店铺账号
     */
    public static void saveShop(ShopAccountDTO shopAccountDTO) {
        TL.set(shopAccountDTO);
    }

    /**
     * 获取当前店铺账号。
     *
     * @return 店铺账号
     */
    public static ShopAccountDTO getShop() {
        return TL.get();
    }

    /**
     * 清理当前店铺账号。
     */
    public static void removeShop() {
        TL.remove();
    }
}
