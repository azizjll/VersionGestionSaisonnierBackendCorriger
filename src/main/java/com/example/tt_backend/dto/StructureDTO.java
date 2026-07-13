package com.example.tt_backend.dto;

public class StructureDTO {

    private Long id;
    private String nom;
    private String type; // EC ou CT
    private String region;
    private String adresse;
    private int autorisesJuillet;
    private        int recrutesJuillet;
    private        int autorisesAout;
    private        int recrutesAout;





    // Constructeur vide (obligatoire pour Jackson)
    public StructureDTO() {
    }

    // Constructeur avec paramètres
    public StructureDTO(Long id, String nom, String type, String region, String adresse, int autorisesJuillet, int recrutesJuillet, int autorisesAout, int recrutesAout) {
        this.id = id;
        this.nom = nom;
        this.type = type;
        this.region = region;
        this.adresse = adresse;
        this.autorisesJuillet = autorisesJuillet;
        this.recrutesJuillet = recrutesJuillet;
        this.autorisesAout = autorisesAout;
        this.recrutesAout = recrutesAout;
    }
    // Getters & Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public int getAutorisesJuillet() {
        return autorisesJuillet;
    }

    public void setAutorisesJuillet(int autorisesJuillet) {
        this.autorisesJuillet = autorisesJuillet;
    }

    public int getRecrutesJuillet() {
        return recrutesJuillet;
    }

    public void setRecrutesJuillet(int recrutesJuillet) {
        this.recrutesJuillet = recrutesJuillet;
    }

    public int getRecrutesAout() {
        return recrutesAout;
    }

    public void setRecrutesAout(int recrutesAout) {
        this.recrutesAout = recrutesAout;
    }

    public int getAutorisesAout() {
        return autorisesAout;
    }

    public void setAutorisesAout(int autorisesAout) {
        this.autorisesAout = autorisesAout;
    }
}