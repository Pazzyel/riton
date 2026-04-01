package com.riton.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.riton.domain.dto.Result;
import com.riton.domain.dto.ShopLoginFormDTO;
import com.riton.domain.entity.ShopAccount;

import jakarta.servlet.http.HttpSession;

/**
 * 店铺账号服务接口。
 */
public interface IShopAccountService extends IService<ShopAccount> {

    /**
     * 发送店铺账号短信验证码。
     *
     * @param phone   手机号
     * @param session 会话
     * @return 发送结果
     */
    Result sendCode(String phone, HttpSession session);

    /**
     * 店铺账号登录，支持密码登录和验证码登录。
     *
     * @param loginForm 登录参数
     * @param session   会话
     * @return 登录结果（token）
     */
    Result login(ShopLoginFormDTO loginForm, HttpSession session);

    /**
     * 店铺账号注册。
     *
     * @param formDTO 注册参数
     * @param session 会话
     * @return 注册结果
     */
    Result register(ShopLoginFormDTO formDTO, HttpSession session);

    /**
     * 店铺账号登出，清除当前账号全部 token。
     *
     * @return 登出结果
     */
    Result logout();

    /**
     * 获取当前登录店铺账号信息。
     *
     * @return 当前店铺账号 DTO
     */
    Result me();
}
