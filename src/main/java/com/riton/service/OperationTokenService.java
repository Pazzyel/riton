package com.riton.service;

import com.riton.domain.dto.Result;

public interface OperationTokenService {

    Result getOperationToken(String requestPath);
}
