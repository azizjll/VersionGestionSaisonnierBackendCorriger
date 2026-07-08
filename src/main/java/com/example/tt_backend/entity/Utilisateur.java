package com.example.tt_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String prenom;


    private String email;


    @Column(nullable = false)
    private boolean mustChangePassword = false;

    private Integer cin;


    private Integer matricule;

    private String telephone;

    @Enumerated(EnumType.STRING)
    private RoleType role;

    @JsonIgnore
    private String password;

    private Boolean enabled = false; // validation par admin ou verification email

    @ManyToOne
    @JoinColumn(name = "region_id")
    private Region region; // obligatoire pour RH, facultatif pour admin ou saisonnier

    @ManyToOne
    @JoinColumn(name = "structure_id")
    private Structure structure;
    // --- Tokens ---
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore
    private VerificationToken verificationToken;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore
    private PasswordResetToken passwordResetToken;



    // Dans Utilisateur.java — ajouter :
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "saisonnier_id")
    @JsonIgnore
    private Saisonnier saisonnier;

    // Dans l'entité Utilisateur — ajouter ce champ :
    @ManyToOne
    @JoinColumn(name = "campagne_id")
    @JsonIgnore
    private Campagne campagne;
}
