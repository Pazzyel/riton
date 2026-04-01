package com.riton.domain.dto;

import lombok.Data;

@Data
public class UserPasswordFormDTO {
    private String oldPassword;
    private String newPassword;
}
