package com.example.tt_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
public class Candidature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate dateDepot;

    @Enumerated(EnumType.STRING)
    private StatutCandidature statut;

    private String commentaire;

    @ManyToOne
    private Saisonnier saisonnier;

    @ManyToOne
    private Campagne campagne;

    @OneToMany(mappedBy = "candidature", cascade = CascadeType.ALL)
    private List<Document> documents;

}