package com.riton.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.riton.domain.dto.LoginFormDTO;
import com.riton.domain.dto.Result;
import com.riton.domain.dto.UserDTO;
import com.riton.domain.dto.UserPasswordFormDTO;
import com.riton.domain.entity.User;
import com.riton.mapper.UserMapper;
import com.riton.service.IUserService;
import com.riton.constants.RedisConstants;
import com.riton.utils.PasswordEncoder;
import com.riton.utils.RegexUtils;
import com.riton.utils.SystemConstants;
import com.riton.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 发送手机验证码
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误!");
        }

        //生成随机六位数字验证码
        String code = RandomUtil.randomNumbers(6);

        //保存到session
        session.setAttribute("code", code);
        log.debug("发送短信验证码成功，验证码: {}", code);
        return Result.ok();
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 第一步：校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误!");
        }

        // 第二步：按参数选择密码登录或验证码登录
        String password = loginForm.getPassword();
        if (StrUtil.isNotBlank(password)) {
            User user = query().eq("phone", phone).one();
            if (user == null) {
                return Result.fail("用户不存在!");
            }
            if (!PasswordEncoder.matches(user.getPassword(), password)) {
                return Result.fail("密码错误!");
            }
            return generateTokenAndReturn(user);
        }

        // session 只有在setAttribute() 后才会创建，并返回客户端一个 JSESSION 用于索引，因此没有获取验证码之前登录会抛出异常
        if (session == null || session.getAttribute("code") == null) {
            return Result.fail("请先获取验证码!");
        }
        String cacheCode = session.getAttribute("code").toString();
        String code = loginForm.getCode();
        if (code == null || !code.equals(cacheCode)) {
            return Result.fail("验证码错误!");
        }

        // 第三步：验证码登录时查用户并按需注册
        User user = query().eq("phone",phone).one();
        if (user == null) {
            user = createUserWithPhone(phone);
        }

        // 第四步：生成并返回 token
        return generateTokenAndReturn(user);
    }

    /**
     * 修改当前登录用户密码
     * @param formDTO 密码修改参数
     * @return 无
     */
    @Override
    public Result updatePassword(UserPasswordFormDTO formDTO) {
        // 第一步：校验登录状态与新密码参数
        UserDTO userDTO = UserHolder.getUser();
        if (userDTO == null) {
            return Result.fail("请先登录!");
        }
        String newPassword = formDTO.getNewPassword();
        if (StrUtil.isBlank(newPassword)) {
            return Result.fail("新密码不能为空!");
        }
        if (newPassword.length() < 6) {
            return Result.fail("新密码长度不能小于6位!");
        }

        // 第二步：校验旧密码（仅当当前账号已设置密码时）
        User user = getById(userDTO.getId());
        if (user == null) {
            return Result.fail("用户不存在!");
        }
        String currentPassword = user.getPassword();
        if (StrUtil.isNotBlank(currentPassword)) {
            if (StrUtil.isBlank(formDTO.getOldPassword())) {
                return Result.fail("请输入旧密码!");
            }
            if (!PasswordEncoder.matches(currentPassword, formDTO.getOldPassword())) {
                return Result.fail("旧密码错误!");
            }
        }

        // 第三步：更新数据库中的加密密码
        user.setPassword(PasswordEncoder.encode(newPassword));
        if (!updateById(user)) {
            return Result.fail("密码更新失败，请稍后重试!");
        }
        return Result.ok();
    }

    /**
     * 清理旧 token 并生成新 token 返回
     * @param user 登录用户
     * @return token
     */
    private Result generateTokenAndReturn(User user) {
        // 第一步：清理用户历史 token
        // session 只有在setAttribute() 后才会创建，并返回客户端一个 JSESSION 用于索引，因此没有获取验证码之前登录会抛出异常
        String userKey = "login:user:" + user.getId();
        Set<String> tokens = stringRedisTemplate.opsForSet().members(userKey);
        if (tokens != null && !tokens.isEmpty()) {
            tokens = tokens.stream().map(token -> RedisConstants.LOGIN_USER_KEY + token).collect(Collectors.toSet());//拼接完整的token
            stringRedisTemplate.delete(tokens);//删除对应的所有token条目
            stringRedisTemplate.delete(userKey);//删除索引表自身
        }

        // 第二步：缓存当前用户并设置 token 有效期
        String token = UUID.randomUUID().toString();
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String,Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName,fieldValue) -> fieldValue.toString()));
        String tokenKey = RedisConstants.LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey,userMap);
        //设置有效期
        stringRedisTemplate.expire(tokenKey,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);

        //另外加一张表维护这个用户的所有token
        stringRedisTemplate.opsForSet().add(userKey,token);

        // 第三步：返回 token
        return Result.ok(token);
    }

    /**
     * 根据手机号创建用户
     * @param phone
     * @return
     */
    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomNumbers(10));
        save(user);
        return user;
    }

    /**
     * 获取当前登录的用户并返回
     * @return 无
     */
    @Override
    public Result me() {
        return Result.ok(UserHolder.getUser());
    }

    /**
     * 登出功能
     * @return 无
     */
    @Override
    public Result logout() {
        Long userId = UserHolder.getUser().getId();
        //清除这个用户存在的所有token
        String userKey = "login:user:" + userId;
        Set<String> tokens = stringRedisTemplate.opsForSet().members(userKey);
        if (tokens != null && !tokens.isEmpty()) {
            tokens = tokens.stream().map(token -> RedisConstants.LOGIN_USER_KEY + token).collect(Collectors.toSet());//拼接完整的token
            stringRedisTemplate.delete(tokens);//删除对应的所有token条目
            stringRedisTemplate.delete(userKey);//删除索引表自身
        }
        UserHolder.removeUser();
        return Result.ok();
    }

    /**
     * 用户签到今天
     * @return 无
     */
    @Override
    public Result sign() {
        Long userId = UserHolder.getUser().getId();
        LocalDateTime now = LocalDateTime.now();
        //拼接key，一个用户一个月的签到信息储存在一张bitMap
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = RedisConstants.USER_SIGN_KEY + userId + keySuffix;
        //设置bitMap储存签到信息
        int day = now.getDayOfMonth();
        //第一个参数key，第二个偏移量，第三个设置的状态（false0，true1）
        stringRedisTemplate.opsForValue().setBit(key,day - 1,true);
        return Result.ok();
    }

    /**
     * 统计用户本月连续签到天数
     * @return 连续签到的天使
     */
    @Override
    public Result signCount() {
        //拼接key，获取日期信息
        Long userId = UserHolder.getUser().getId();
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = RedisConstants.USER_SIGN_KEY + userId + keySuffix;
        int day = now.getDayOfMonth();
        //从Redis查询BitMap信息,注意Redis只支持i8和u16，因此BitFieldSubCommands.BitFieldType.unsigned(day)来把长度转化为支持的格式
        //get(...) 表示要从 Redis 中 读取 bit 值。
        //BitFieldType.unsigned(day)：表示要以 无符号整数 的方式读取，长度是 day 个 bit。
        //valueAt(0)：从 bit 偏移量 0 开始读（即从第 0 位开始）
        List<Long> bitMaps = stringRedisTemplate.opsForValue().bitField(key,
                BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(day)).valueAt(0));
        if(bitMaps == null || bitMaps.isEmpty()){
            return Result.ok(0);//没有任何签到结果
        }
        long num = bitMaps.get(0);
        if(num == 0){
            return Result.ok(0);
        }
        //循环遍历连续签到天数
        int count = 0;
        while(num > 0){
            if((num & 1) == 1){
                ++count;
            } else {
                //断签
                break;
            }
            num >>>= 1;//逻辑右移才不考虑符号位
        }
        return Result.ok(count);
    }

}
