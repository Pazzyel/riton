package com.riton.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "pay")
public class PayProperties {

    private String refundReason = "order status invalid when payment callback";

    private Alipay alipay = new Alipay();

    private Wechat wechat = new Wechat();

    @Data
    public static class Alipay {
        private String gatewayUrl;
        private String appId;
        private String privateKey;
        private String alipayPublicKey;
        private String charset = "UTF-8";
        private String signType = "RSA2";
        private String notifyUrl;
    }

    @Data
    public static class Wechat {
        private String mchId;
        private String appId;
        private String privateKeyPath;
        private String merchantSerialNumber;
        private String apiV3Key;
        private String notifyUrl;
    }
}
