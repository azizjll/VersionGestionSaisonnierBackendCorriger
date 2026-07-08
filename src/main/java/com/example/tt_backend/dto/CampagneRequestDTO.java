package com.example.tt_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor

public class CampagneRequestDTO {

    private String libelle;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Double budget;
    private String description;
    private String code;
    private List<Long> regionIds;
    private String statut;
}
