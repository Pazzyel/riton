package com.riton.shop;

import com.riton.controller.shop.ShopAuthController;
import com.riton.domain.dto.Result;
import com.riton.domain.dto.ShopLoginFormDTO;
import com.riton.interceptor.ShopLoginInterceptor;
import com.riton.service.IShopAccountService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 店铺认证流程最小可执行测试。
 */
public class ShopAuthFlowTest {

    /**
     * 未登录访问 /shop/auth/me 的拦截器行为应返回 401。
     *
     * @throws Exception 反射或拦截异常
     */
    @Test
    public void shouldUnauthorizedWhenMeWithoutToken() throws Exception {
        ShopLoginInterceptor interceptor = new ShopLoginInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/shop/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        Assertions.assertFalse(allowed);
        Assertions.assertEquals(401, response.getStatus());
    }

    /**
     * 登录路径桩测试：控制器应调用服务并返回业务结果。
     */
    @Test
    public void shouldReturnServiceResultWhenLoginInvoked() {
        // 第一步：构造服务桩并固定返回失败结果。
        Result expected = Result.fail("手机号格式错误!");
        IShopAccountService shopAccountService = (IShopAccountService) Proxy.newProxyInstance(
                IShopAccountService.class.getClassLoader(),
                new Class[]{IShopAccountService.class},
                (proxy, method, args) -> {
                    if ("login".equals(method.getName())) {
                        return expected;
                    }
                    return defaultValue(method.getReturnType());
                });

        // 第二步：通过反射注入服务并调用登录接口。
        ShopAuthController controller = new ShopAuthController(shopAccountService);
        ShopLoginFormDTO formDTO = new ShopLoginFormDTO();
        setField(formDTO, "phone", "123");
        setField(formDTO, "code", "123456");
        Result actual = controller.login(formDTO, null);

        // 第三步：断言返回与桩结果一致。
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(expected.getSuccess(), actual.getSuccess());
        Assertions.assertEquals(expected.getErrorMsg(), actual.getErrorMsg());
    }

    /**
     * 反射写入对象字段值。
     *
     * @param target    目标对象
     * @param fieldName 字段名
     * @param value     字段值
     */
    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("反射设置字段失败", e);
        }
    }

    /**
     * 返回方法返回类型的默认值。
     *
     * @param returnType 返回类型
     * @return 默认值
     */
    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(returnType)) {
            return false;
        }
        if (char.class.equals(returnType)) {
            return '\0';
        }
        return 0;
    }
}
