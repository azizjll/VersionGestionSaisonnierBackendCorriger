package com.example.tt_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ParentAutorise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomPrenom;


    private String matricule;
    private String email;
    private int autorises;   // nombre max d'utilisations (depuis Excel)
    private int utilise = 0; // 🔥 pour savoir si déjà utilisé

    @ManyToOne
    @JoinColumn(name = "campagne_id")
    @JsonIgnore
    private Campagne campagne;
}
