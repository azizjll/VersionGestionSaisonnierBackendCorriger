package com.example.tt_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class EtatRHDTO {
    private Long id;
    private String url;
    private String nomFichier;
    private String statut;
    private LocalDateTime dateUpload;
    private String regionNom;
    private String rhNom;      // nom du RH qui a soumis
    private Long campagneId;
}