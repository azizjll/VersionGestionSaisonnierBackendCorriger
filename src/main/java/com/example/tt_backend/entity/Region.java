package com.example.tt_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String nom;

    @OneToMany(mappedBy = "region")
    @JsonIgnore
    private List<Utilisateur> utilisateurs;

    @OneToMany(mappedBy = "region")
    @JsonIgnore
    private List<Saisonnier> saisonniers;

    @ManyToMany(mappedBy = "regions")
    @JsonIgnore
    private List<Campagne> campagnes;

    @OneToMany(mappedBy = "region")
    @JsonIgnore
    private List<Structure> structures;

    public Region() {}

    public Region(String nom) {
        this.nom = nom;
    }
}