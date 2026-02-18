package com.riton.service;

import com.riton.dto.PaymentDTO;
import com.riton.dto.Result;

import java.util.Map;

public interface IPayService {

    Result pay(PaymentDTO paymentDTO);

    String alipayCallback(Map<String, String> callbackParams);

    String wechatCallback(String body, String nonce, String signature, String serial, String timestamp);
}
