package com.example.tt_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NewPasswordRequest {
    private String token;
    private String newPassword;
}
