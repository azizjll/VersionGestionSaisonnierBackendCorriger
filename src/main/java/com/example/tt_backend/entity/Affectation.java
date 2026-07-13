package com.example.tt_backend.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
public class Affectation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate dateAffectation;

    private String moisTravail; // "JUILLET" ou "AOUT"


    @ManyToOne
    @JoinColumn(name = "campagne_id")
    private Campagne campagne;

    @ManyToOne
    @JoinColumn(name = "structure_id")
    private Structure structure;

    @ManyToOne
    @JoinColumn(name = "saisonnier_id")
    private Saisonnier saisonnier;

    @OneToOne
    @JoinColumn(name = "candidature_id", unique = true)
    private Candidature candidature;

}