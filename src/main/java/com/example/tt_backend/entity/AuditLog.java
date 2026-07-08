package com.example.tt_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String utilisateurEmail;

    private String action;           // CREATE, UPDATE, DELETE, ACTIVER, CLOTURER

    private String entite;           // "Campagne", "Structure", etc.

    private Long entiteId;           // id de l'objet concerné

    @Column(columnDefinition = "TEXT")
    private String donneesAvant;     // JSON sérialisé avant modification

    @Column(columnDefinition = "TEXT")
    private String donneesApres;     // JSON sérialisé après modification

    private String adresseIp;

    private String statut;           // SUCCESS, FAILURE

    private LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        this.timestamp = LocalDateTime.now();
    }
}