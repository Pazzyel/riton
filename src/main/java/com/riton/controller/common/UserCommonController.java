package com.riton.controller.common;

import cn.hutool.core.bean.BeanUtil;
import com.riton.domain.dto.LoginFormDTO;
import com.riton.domain.dto.Result;
import com.riton.domain.dto.UserDTO;
import com.riton.domain.entity.User;
import com.riton.domain.entity.UserInfo;
import com.riton.service.IUserInfoService;
import com.riton.service.IUserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户公共接口控制器。
 */
@RestController
@RequestMapping("/user")
public class UserCommonController {

    private final IUserService userService;
    private final IUserInfoService userInfoService;

    /**
     * 构造用户公共接口控制器。
     *
     * @param userService     用户服务
     * @param userInfoService 用户详情服务
     */
    @Autowired
    public UserCommonController(IUserService userService, IUserInfoService userInfoService) {
        this.userService = userService;
        this.userInfoService = userInfoService;
    }

    /**
     * 发送手机验证码。
     *
     * @param phone   手机号
     * @param session 会话
     * @return 发送结果
     */
    @PostMapping("/code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        return userService.sendCode(phone, session);
    }

    /**
     * 用户登录。
     *
     * @param loginForm 登录参数
     * @param session   会话
     * @return 登录结果
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session) {
        return userService.login(loginForm, session);
    }

    /**
     * 查询用户详情信息。
     *
     * @param userId 用户 ID
     * @return 用户详情
     */
    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId) {
        // 第一步：查询用户详情。
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            return Result.ok();
        }

        // 第二步：对外隐藏创建和更新时间字段。
        Map<String, Object> infoMap = BeanUtil.beanToMap(info);
        infoMap.remove("createTime");
        infoMap.remove("updateTime");

        // 第三步：返回用户详情。
        return Result.ok(infoMap);
    }

    /**
     * 根据 ID 查询用户基础信息。
     *
     * @param userId 用户 ID
     * @return 用户基础信息
     */
    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable("id") Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        return Result.ok(userDTO);
    }
}
