package com.riton.controller.shop;

import com.riton.domain.dto.Result;
import com.riton.domain.dto.ShopLoginFormDTO;
import com.riton.service.IShopAccountService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 店铺账号认证控制器。
 */
@RestController
@RequestMapping("/shop/auth")
public class ShopAuthController {

    private final IShopAccountService shopAccountService;

    /**
     * 构造店铺账号认证控制器。
     *
     * @param shopAccountService 店铺账号服务
     */
    @Autowired
    public ShopAuthController(IShopAccountService shopAccountService) {
        this.shopAccountService = shopAccountService;
    }

    /**
     * 发送店铺账号验证码。
     *
     * @param phone   手机号
     * @param session 会话
     * @return 发送结果
     */
    @PostMapping("/code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        return shopAccountService.sendCode(phone, session);
    }

    /**
     * 店铺账号登录。
     *
     * @param loginForm 登录参数
     * @param session   会话
     * @return 登录结果
     */
    @PostMapping("/login")
    public Result login(@RequestBody ShopLoginFormDTO loginForm, HttpSession session) {
        return shopAccountService.login(loginForm, session);
    }

    /**
     * 店铺账号注册。
     *
     * @param formDTO 注册参数
     * @param session 会话
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result register(@RequestBody ShopLoginFormDTO formDTO, HttpSession session) {
        return shopAccountService.register(formDTO, session);
    }

    /**
     * 店铺账号登出。
     *
     * @return 登出结果
     */
    @PostMapping("/logout")
    public Result logout() {
        return shopAccountService.logout();
    }

    /**
     * 获取当前店铺账号信息。
     *
     * @return 当前店铺账号
     */
    @GetMapping("/me")
    public Result me() {
        return shopAccountService.me();
    }
}
