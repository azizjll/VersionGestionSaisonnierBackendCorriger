package com.example.tt_backend.dto;

import lombok.Data;

@Data
public class DocumentCampagneDTO {

    private Long id;
    private String nom;
    private String type;
    private String url;
    private Long campagneId;
}
