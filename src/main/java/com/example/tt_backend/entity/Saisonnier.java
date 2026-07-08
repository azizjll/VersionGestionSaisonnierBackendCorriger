package com.example.tt_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
public class Saisonnier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String prenom;

    @Column(unique = true)
    private String cin;

    private String rib;
    private String telephone;
    private String email;

    private String nomPrenomParent;
    private String matriculeParent;
    private String niveauEtude;
    private String diplome;
    private String specialiteDiplome;

    private String moisTravail; //

    private Integer absences = 0;


    @ManyToOne
    @JoinColumn(name = "region_id")
    private Region region;

    // 🔹 relation avec candidatures
    @OneToMany(mappedBy = "saisonnier", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Candidature> candidatures;

    // Dans Saisonnier.java — ajouter le lien inverse (optionnel) :
    @OneToOne(mappedBy = "saisonnier")
    @JsonIgnore
    private Utilisateur utilisateur;

    // ✅ S1186 — Commentaire expliquant pourquoi le constructeur est vide
    public Saisonnier() {
        // Constructeur vide requis par JPA
    }

}
