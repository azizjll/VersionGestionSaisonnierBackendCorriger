package com.example.tt_backend.service;

import com.example.tt_backend.entity.*;
import com.example.tt_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor // ✅ S6813 — remplace les @Autowired sur les champs
public class AffectationService {

    private final AffectationRepository affectationRepository;
    private final SaisonnierRepository saisonnierRepository;
    private final StructureRepository structureRepository;
    private final CampagneRepository campagneRepository;

    public void affecterSaisonnier(Long saisonnierId, Long structureId, Long campagneId) {

        Saisonnier saisonnier = saisonnierRepository.findById(saisonnierId)
                .orElseThrow(() -> new NoSuchElementException("Saisonnier introuvable"));

        Structure structure = structureRepository.findById(structureId)
                .orElseThrow(() -> new NoSuchElementException("Structure introuvable"));

        Campagne campagne = campagneRepository.findById(campagneId)
                .orElseThrow(() -> new NoSuchElementException("Campagne introuvable"));

        // ✅ S112 — IllegalArgumentException au lieu de RuntimeException
        if (!saisonnier.getRegion().getId().equals(structure.getRegion().getId())) {
            throw new IllegalArgumentException("Région invalide");
        }

        Affectation aff = new Affectation();
        aff.setSaisonnier(saisonnier);
        aff.setStructure(structure);
        aff.setCampagne(campagne);
        aff.setDateAffectation(LocalDate.now());

        affectationRepository.save(aff);
    }
}