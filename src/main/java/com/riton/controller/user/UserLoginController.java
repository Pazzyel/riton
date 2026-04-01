package com.riton.controller.user;

import com.riton.domain.dto.Result;
import com.riton.domain.dto.UserPasswordFormDTO;
import com.riton.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户登录后接口控制器。
 */
@RestController
@RequestMapping("/user")
public class UserLoginController {

    private final IUserService userService;

    /**
     * 构造用户登录后接口控制器。
     *
     * @param userService 用户服务
     */
    @Autowired
    public UserLoginController(IUserService userService) {
        this.userService = userService;
    }

    /**
     * 用户登出。
     *
     * @return 登出结果
     */
    @PostMapping("/logout")
    public Result logout() {
        return userService.logout();
    }

    /**
     * 获取当前登录用户信息。
     *
     * @return 当前用户信息
     */
    @GetMapping("/me")
    public Result me() {
        return userService.me();
    }

    /**
     * 用户签到。
     *
     * @return 签到结果
     */
    @PostMapping("/sign")
    public Result sign() {
        return userService.sign();
    }

    /**
     * 查询本月连续签到天数。
     *
     * @return 连续签到天数
     */
    @GetMapping("/sign/count")
    public Result signCount() {
        return userService.signCount();
    }

    /**
     * 修改当前登录用户密码。
     *
     * @param formDTO 密码修改参数
     * @return 修改结果
     */
    @PutMapping("/password")
    public Result updatePassword(@RequestBody UserPasswordFormDTO formDTO) {
        return userService.updatePassword(formDTO);
    }
}
