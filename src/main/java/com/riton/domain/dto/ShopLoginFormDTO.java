package com.riton.domain.dto;

import lombok.Data;

/**
 * 店铺账号登录/注册参数。
 */
@Data
public class ShopLoginFormDTO {

    /**
     * 手机号。
     */
    private String phone;

    /**
     * 验证码。
     */
    private String code;

    /**
     * 密码。
     */
    private String password;

    /**
     * 店铺 ID（注册必填）。
     */
    private Long shopId;
}
