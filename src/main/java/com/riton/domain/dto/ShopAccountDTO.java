package com.riton.domain.dto;

import lombok.Data;

/**
 * 店铺账号登录态 DTO。
 */
@Data
public class ShopAccountDTO {

    /**
     * 店铺账号 ID。
     */
    private Long id;

    /**
     * 店铺 ID。
     */
    private Long shopId;

    /**
     * 手机号。
     */
    private String phone;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
