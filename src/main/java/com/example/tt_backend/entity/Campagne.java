package com.example.tt_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
public class Campagne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String libelle;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Double budget;
    private String description;
    private String code;

    @Enumerated(EnumType.STRING)
    private StatutCampagne statut;

    // Une campagne peut concerner plusieurs régions
    @ManyToMany
    @JoinTable(
            name = "campagne_region",
            joinColumns = @JoinColumn(name = "campagne_id"),
            inverseJoinColumns = @JoinColumn(name = "region_id")
    )
    @JsonIgnore
    private List<Region> regions;

    @ManyToOne
    @JoinColumn(name = "createur_id")
    private Utilisateur createur;

    @OneToMany(mappedBy = "campagne", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<DocumentCampagne> documents;
}