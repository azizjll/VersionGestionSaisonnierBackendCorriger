package com.example.tt_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
@Data
@AllArgsConstructor
public class DepotCandidatureRequest {

    private String nom;
    private String prenom;
    private Integer cin;
    private String rib;
    private String telephone;
    private String email;

    private Long regionId;
    private Long campagneId;

    private MultipartFile cinFile;
    private MultipartFile diplome;
    private MultipartFile contrat;
}