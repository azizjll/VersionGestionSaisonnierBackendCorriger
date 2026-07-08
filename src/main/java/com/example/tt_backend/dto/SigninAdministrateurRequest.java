package com.example.tt_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SigninAdministrateurRequest {

    private Integer matricule;

    private String password;
}