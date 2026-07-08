package com.example.tt_backend.dto;

public class StructureDTO {

    private Long id;
    private String nom;
    private String type; // EC ou CT
    private String region;
    private String adresse;
    private Integer autorises;
    private Integer recrutes;


    public boolean isDisponible() {
        if (autorises == null || recrutes == null) return true;
        return recrutes < autorises;
    }


    // Constructeur vide (obligatoire pour Jackson)
    public StructureDTO() {
    }

    // Constructeur avec paramètres
    public StructureDTO(Long id, String nom, String type, String region, String adresse, Integer autorises, Integer recrutes) {
        this.id = id;
        this.nom = nom;
        this.type = type;
        this.region = region;
        this.adresse = adresse;
        this.autorises = autorises;
        this.recrutes = recrutes;
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

    public Integer getAutorises() {
        return autorises;
    }

    public void setAutorises(Integer autorises) {
        this.autorises = autorises;
    }

    public Integer getRecrutes() {
        return recrutes;
    }

    public void setRecrutes(Integer recrutes) {
        this.recrutes = recrutes;
    }
}