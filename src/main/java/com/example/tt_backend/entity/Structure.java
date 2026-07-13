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

    // ❌ on retire les anciens champs autorises / recrutes globaux
    @Column(columnDefinition = "INT DEFAULT 0")
    private int autorisesJuillet;

    @Column(columnDefinition = "INT DEFAULT 0")
    private int autorisesAout;

    @Column(columnDefinition = "INT DEFAULT 0")
    private int recrutesJuillet;

    @Column(columnDefinition = "INT DEFAULT 0")
    private int recrutesAout;

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
    // ── totaux calculés, utiles pour l'affichage global si besoin ──
    public int getAutorisesTotal() { return autorisesJuillet + autorisesAout; }
    public int getRecrutesTotal()  { return recrutesJuillet + recrutesAout; }

    // ── Disponibilité globale : au moins un des deux mois a de la place ──
    public boolean isDisponiblePourCandidature() {
        return (recrutesJuillet < autorisesJuillet) || (recrutesAout < autorisesAout);
    }

    public boolean isDisponiblePourMois(String mois) {
        if ("JUILLET".equals(mois)) return recrutesJuillet < autorisesJuillet;
        return recrutesAout < autorisesAout;
    }}
