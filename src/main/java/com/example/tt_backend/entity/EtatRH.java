package com.example.tt_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class EtatRH {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String url;           // URL Cloudinary du PDF
    private String nomFichier;
    private LocalDateTime dateUpload;

    @Enumerated(EnumType.STRING)
    private StatutEtat statut = StatutEtat.SOUMIS; // SOUMIS, VALIDE, REJETE

    @ManyToOne
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;   // le RH_REGIONAL

    @ManyToOne
    @JoinColumn(name = "campagne_id")
    private Campagne campagne;

    @ManyToOne
    @JoinColumn(name = "region_id")
    private Region region;             // dénormalisé pour requête facile
}