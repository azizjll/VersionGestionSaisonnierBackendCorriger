package com.example.tt_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class DocumentCampagne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom; // ex: "Engagement", "Note officielle"

    private String url;

    private String type; // optionnel

    @ManyToOne
    @JoinColumn(name = "campagne_id")
    @JsonIgnore
    private Campagne campagne;
}