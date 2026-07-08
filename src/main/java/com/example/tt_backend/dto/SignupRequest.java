package com.example.tt_backend.dto;

import com.example.tt_backend.entity.RoleType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {
    private String nom;
    private String prenom;
    private String email;
    private Integer cin;
    private Integer matricule;    // Matricule professionnel
    private String telephone;
    private String password;
    private RoleType role;
    private Long regionId; // facultatif selon rôle
}
