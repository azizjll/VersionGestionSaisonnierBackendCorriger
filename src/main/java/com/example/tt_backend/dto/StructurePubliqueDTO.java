package com.example.tt_backend.dto;

public record StructurePubliqueDTO(
        Long id,
        String nom,
        String type,
        String region,
        String adresse,
        int autorisesJuillet,
        int recrutesJuillet,
        int autorisesAout,
        int recrutesAout
) {}
