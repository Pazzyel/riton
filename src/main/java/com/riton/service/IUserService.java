package com.riton.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.riton.domain.dto.LoginFormDTO;
import com.riton.domain.dto.Result;
import com.riton.domain.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {
    /**
     * 发送手机验证码
     */
    Result sendCode(String phone, HttpSession session);

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    Result login(LoginFormDTO loginForm, HttpSession session);

    /**
     * 获取当前登录的用户并返回
     */
    Result me();

    /**
     * 登出功能
     * @return 无
     */
    Result logout();

    /**
     * 用户签到今天
     * @return 无
     */
    Result sign();

    /**
     * 统计用户本月连续签到天数
     * @return 连续签到的天使
     */
    Result signCount();
}
