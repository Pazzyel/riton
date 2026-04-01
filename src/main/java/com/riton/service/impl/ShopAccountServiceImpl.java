package com.riton.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.riton.constants.RedisConstants;
import com.riton.domain.dto.Result;
import com.riton.domain.dto.ShopAccountDTO;
import com.riton.domain.dto.ShopLoginFormDTO;
import com.riton.domain.entity.Shop;
import com.riton.domain.entity.ShopAccount;
import com.riton.mapper.ShopAccountMapper;
import com.riton.service.IShopAccountService;
import com.riton.service.IShopService;
import com.riton.utils.PasswordEncoder;
import com.riton.utils.RegexUtils;
import com.riton.utils.ShopHolder;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 店铺账号服务实现。
 */
@Service
public class ShopAccountServiceImpl extends ServiceImpl<ShopAccountMapper, ShopAccount> implements IShopAccountService {

    private final StringRedisTemplate stringRedisTemplate;
    private final IShopService shopService;

    @Autowired
    /**
     * 构造店铺账号服务实现。
     *
     * @param stringRedisTemplate Redis 模板
     * @param shopService         店铺服务
     */
    public ShopAccountServiceImpl(StringRedisTemplate stringRedisTemplate, IShopService shopService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.shopService = shopService;
    }

    /**
     * 发送店铺账号短信验证码。
     *
     * @param phone   手机号
     * @param session 会话
     * @return 发送结果
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 第一步：校验手机号格式。
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误!");
        }

        // 第二步：生成验证码并写入 Redis。
        String code = RandomUtil.randomNumbers(6);
        String codeKey = RedisConstants.LOGIN_SHOP_CODE_KEY + phone;
        stringRedisTemplate.opsForValue().set(codeKey, code, RedisConstants.LOGIN_SHOP_CODE_TTL, TimeUnit.MINUTES);
        return Result.ok();
    }

    /**
     * 店铺账号登录，支持密码登录和验证码登录。
     *
     * @param loginForm 登录参数
     * @param session   会话
     * @return 登录结果（token）
     */
    @Override
    public Result login(ShopLoginFormDTO loginForm, HttpSession session) {
        // 第一步：校验手机号。
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误!");
        }

        // 第二步：根据参数决定密码登录或验证码登录。
        String password = loginForm.getPassword();
        ShopAccount shopAccount = query().eq("phone", phone).one();
        if (shopAccount == null) {
            return Result.fail("店铺账号不存在，请先注册!");
        }
        if (Integer.valueOf(0).equals(shopAccount.getStatus())) {
            return Result.fail("账号已被禁用，请联系管理员!");
        }

        if (StrUtil.isNotBlank(password)) {
            if (!PasswordEncoder.matches(shopAccount.getPassword(), password)) {
                return Result.fail("密码错误!");
            }
            return generateTokenAndReturn(shopAccount);
        }

        // 第三步：验证码登录校验验证码。
        String codeKey = RedisConstants.LOGIN_SHOP_CODE_KEY + phone;
        String cacheCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (StrUtil.isBlank(cacheCode)) {
            return Result.fail("请先获取验证码!");
        }
        String code = loginForm.getCode();
        if (StrUtil.isBlank(code) || !code.equals(cacheCode)) {
            return Result.fail("验证码错误!");
        }

        // 第四步：登录成功后清理验证码并返回 token。
        stringRedisTemplate.delete(codeKey);
        return generateTokenAndReturn(shopAccount);
    }

    /**
     * 店铺账号注册。
     *
     * @param formDTO 注册参数
     * @param session 会话
     * @return 注册结果
     */
    @Override
    public Result register(ShopLoginFormDTO formDTO, HttpSession session) {
        // 第一步：校验基础参数。
        String phone = formDTO.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误!");
        }
        if (formDTO.getShopId() == null) {
            return Result.fail("店铺ID不能为空!");
        }
        if (StrUtil.isBlank(formDTO.getPassword())) {
            return Result.fail("密码不能为空!");
        }
        if (StrUtil.isBlank(formDTO.getCode())) {
            return Result.fail("验证码不能为空!");
        }

        // 第二步：校验店铺是否存在。
        Shop shop = shopService.getById(formDTO.getShopId());
        if (shop == null) {
            return Result.fail("店铺不存在!");
        }

        // 第三步：校验验证码和账号重复。
        String codeKey = RedisConstants.LOGIN_SHOP_CODE_KEY + phone;
        String cacheCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (StrUtil.isBlank(cacheCode) || !formDTO.getCode().equals(cacheCode)) {
            return Result.fail("验证码错误!");
        }
        ShopAccount existed = query().eq("phone", phone).one();
        if (existed != null) {
            return Result.fail("手机号已注册店铺账号!");
        }
        ShopAccount existedByShop = query().eq("shop_id", formDTO.getShopId()).one();
        if (existedByShop != null) {
            return Result.fail("该店铺已存在账号，请勿重复注册!");
        }

        // 第四步：创建账号并清理验证码。
        ShopAccount shopAccount = new ShopAccount();
        shopAccount.setPhone(phone);
        shopAccount.setShopId(formDTO.getShopId());
        shopAccount.setPassword(PasswordEncoder.encode(formDTO.getPassword()));
        shopAccount.setStatus(1);
        save(shopAccount);
        stringRedisTemplate.delete(codeKey);
        return Result.ok();
    }

    /**
     * 店铺账号登出，清除当前账号全部 token。
     *
     * @return 登出结果
     */
    @Override
    public Result logout() {
        // 第一步：读取当前登录店铺账号。
        ShopAccountDTO shopAccountDTO = ShopHolder.getShop();
        if (shopAccountDTO == null) {
            return Result.fail("请先登录!");
        }

        // 第二步：删除当前账号全部 token 及索引。
        String shopKey = RedisConstants.LOGIN_SHOP_INDEX_KEY + shopAccountDTO.getId();
        Set<String> tokens = stringRedisTemplate.opsForSet().members(shopKey);
        if (tokens != null && !tokens.isEmpty()) {
            Set<String> tokenKeys = tokens.stream()
                    .map(token -> RedisConstants.LOGIN_SHOP_KEY + token)
                    .collect(Collectors.toSet());
            stringRedisTemplate.delete(tokenKeys);
            stringRedisTemplate.delete(shopKey);
        }

        // 第三步：清理上下文并返回。
        ShopHolder.removeShop();
        return Result.ok();
    }

    /**
     * 获取当前登录店铺账号信息。
     *
     * @return 当前店铺账号 DTO
     */
    @Override
    public Result me() {
        return Result.ok(ShopHolder.getShop());
    }

    /**
     * 清理旧 token 后生成新 token。
     *
     * @param shopAccount 店铺账号
     * @return token 结果
     */
    private Result generateTokenAndReturn(ShopAccount shopAccount) {
        // 第一步：清理店铺账号历史 token。
        String shopKey = RedisConstants.LOGIN_SHOP_INDEX_KEY + shopAccount.getId();
        Set<String> tokens = stringRedisTemplate.opsForSet().members(shopKey);
        if (tokens != null && !tokens.isEmpty()) {
            Set<String> tokenKeys = tokens.stream()
                    .map(token -> RedisConstants.LOGIN_SHOP_KEY + token)
                    .collect(Collectors.toSet());
            stringRedisTemplate.delete(tokenKeys);
            stringRedisTemplate.delete(shopKey);
        }

        // 第二步：写入当前 token 的登录态信息。
        String token = UUID.randomUUID().toString();
        ShopAccountDTO dto = BeanUtil.copyProperties(shopAccount, ShopAccountDTO.class);
        Map<String, Object> map = BeanUtil.beanToMap(dto, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        String tokenKey = RedisConstants.LOGIN_SHOP_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, map);
        stringRedisTemplate.expire(tokenKey, RedisConstants.LOGIN_SHOP_TTL, TimeUnit.MINUTES);

        // 第三步：维护 token 索引并返回 token。
        stringRedisTemplate.opsForSet().add(shopKey, token);
        return Result.ok(token);
    }
}
