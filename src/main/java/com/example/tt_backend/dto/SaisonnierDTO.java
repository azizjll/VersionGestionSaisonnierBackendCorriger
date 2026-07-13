package com.example.tt_backend.dto;

import com.example.tt_backend.entity.Saisonnier;
import lombok.Data;

@Data
public class SaisonnierDTO {
    private Long id;
    private String nom;
    private String prenom;
    private String cin;
    private String rib;
    private String statut;
    private String moisTravail;
    // ✅ S1104 — private au lieu de public (Lombok @Data génère le getter/setter)
    private Integer absences;
    private String regionNom; // 🆕 Direction / Région

    public static SaisonnierDTO from(Saisonnier s, String statut) {
        SaisonnierDTO dto = new SaisonnierDTO();
        dto.setId(s.getId());
        dto.setNom(s.getNom());
        dto.setPrenom(s.getPrenom());
        dto.setCin(String.valueOf(s.getCin()));
        dto.setRib(s.getRib());
        dto.setStatut(statut);
        dto.setMoisTravail(s.getMoisTravail());
        // ✅ Utiliser le setter généré par @Data au lieu d'accès direct au champ public
        dto.setAbsences(s.getAbsences() != null ? s.getAbsences() : 0);
        dto.setRegionNom(s.getRegion() != null ? s.getRegion().getNom() : null); // 🆕
        return dto;
    }
}