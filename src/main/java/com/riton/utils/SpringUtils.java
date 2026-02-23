package com.riton.utils;

import com.riton.constants.Constants;
import lombok.Getter;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

@Deprecated
public class SpringUtils implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static ConfigurableApplicationContext configurableApplicationContext;

    /**
     * -- GETTER --
     *  从配置文件读出前缀配置
     *
     * @return 前缀配置名
     */
    @Getter
    public static String prefixDistinctionName;


    @Override
    public void initialize(final ConfigurableApplicationContext applicationContext) {
        configurableApplicationContext = applicationContext;
        prefixDistinctionName = configurableApplicationContext.getEnvironment().getProperty(Constants.PREFIX_DISTINCTION_NAME,
                Constants.DEFAULT_PREFIX_DISTINCTION_NAME);
    }
}
