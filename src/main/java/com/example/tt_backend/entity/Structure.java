package com.example.tt_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Entity
@Getter
@Setter
public class Structure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom; // nom précis de l'espace ou centre

    @Enumerated(EnumType.STRING)
    private StructureType type; // Ajout du type

    private String adresse;

    @Column(columnDefinition = "INT DEFAULT 3")
    private int autorises ;

    @Column(columnDefinition = "INT DEFAULT 0")
    private int recrutes;

    @ManyToOne
    @JoinColumn(name = "region_id")
    @JsonIgnore
    private Region region;

    @OneToMany(mappedBy = "structure")
    @JsonIgnore
    private List<Affectation> affectations;
    @ManyToOne                          // ← NOUVEAU
    @JoinColumn(name = "campagne_id")   // ← NOUVEAU
    @JsonIgnore                         // ← NOUVEAU
    private Campagne campagne;

    // constructeur vide obligatoire
    public Structure() {}

    // ✅ Constructeur utilisé dans DataLoader
    public Structure(Long id, String nom, StructureType type, Region region) {
        this.id = id;
        this.nom = nom;
        this.type = type;
        this.region = region;
    }
    public boolean isDisponiblePourCandidature() {
    return this.recrutes < this.autorises;
}
}
