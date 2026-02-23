package com.riton.controller;


import cn.hutool.core.bean.BeanUtil;
import com.riton.domain.dto.LoginFormDTO;
import com.riton.domain.dto.Result;
import com.riton.domain.dto.UserDTO;
import com.riton.domain.entity.User;
import com.riton.domain.entity.UserInfo;
import com.riton.service.IUserInfoService;
import com.riton.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private IUserService userService;

    @Autowired
    private IUserInfoService userInfoService;

    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        return userService.sendCode(phone, session);
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){
        return userService.login(loginForm, session);
    }

    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout(){
        return userService.logout();
    }

    /**
     * 获取当前登录的用户并返回
     * @return 无
     */
    @GetMapping("/me")
    public Result me(){
        return userService.me();
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }

    /**
     * 根据id查询用户
     * @param userId 用户id
     * @return 用户
     */
    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable("id") Long userId){
        User user = userService.getById(userId);
        if(user == null){
            return Result.ok();
        }

        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        return Result.ok(userDTO);
    }

    /**
     * 用户签到今天
     * @return
     */
    @PostMapping("/sign")
    public Result sign(){
        return userService.sign();
    }

    /**
     * 统计用户本月连续签到天数
     * @return 连续签到的天使
     */
    @GetMapping("/sign/count")
    public Result signCount(){
        return userService.signCount();
    }
}
