package com.example.tt_backend.dto;
import java.time.LocalDate;

public record CampagnePubliqueDTO(
        Long id,
        String libelle,
        LocalDate dateDebut,
        LocalDate dateFin,
        String statut
) {}
