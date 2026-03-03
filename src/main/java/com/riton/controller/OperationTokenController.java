package com.riton.controller;

import com.riton.domain.dto.Result;
import com.riton.service.OperationTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/optoken")
public class OperationTokenController {

    private OperationTokenService operationTokenService;

    @Autowired
    public OperationTokenController(OperationTokenService operationTokenService) {
        this.operationTokenService = operationTokenService;
    }

    /**
     * 对幂等操作，需要前端先请求唯一token，这是请求唯一token的接口
     * @param requestPath 访问API的URI
     * @return 唯一token
     */
    @GetMapping
    public Result getOperationToken(@RequestParam("requestPath") String requestPath) {
        return operationTokenService.getOperationToken(requestPath);
    }

}
