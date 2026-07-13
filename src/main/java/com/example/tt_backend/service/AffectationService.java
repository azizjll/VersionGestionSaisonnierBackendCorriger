package com.example.tt_backend.service;

import com.example.tt_backend.entity.*;
import com.example.tt_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class AffectationService {

    private final AffectationRepository affectationRepository;
    private final CandidatureRepository candidatureRepository;
    private final StructureRepository structureRepository;
    private final CampagneRepository campagneRepository;

    public void affecterCandidature(Long candidatureId, Long structureId, Long campagneId) {

        Candidature candidature = candidatureRepository.findById(candidatureId)
                .orElseThrow(() -> new NoSuchElementException("Candidature introuvable"));

        Structure structure = structureRepository.findById(structureId)
                .orElseThrow(() -> new NoSuchElementException("Structure introuvable"));

        Campagne campagne = campagneRepository.findById(campagneId)
                .orElseThrow(() -> new NoSuchElementException("Campagne introuvable"));

        Saisonnier saisonnier = candidature.getSaisonnier();

        if (!saisonnier.getRegion().getId().equals(structure.getRegion().getId())) {
            throw new IllegalArgumentException("Région invalide");
        }

        // ✅ upsert : une candidature = une seule affectation
        Affectation aff = affectationRepository.findByCandidatureId(candidatureId)
                .orElse(new Affectation());

        aff.setCandidature(candidature);
        aff.setSaisonnier(saisonnier);
        aff.setStructure(structure);
        aff.setCampagne(campagne);
        aff.setDateAffectation(LocalDate.now());
        aff.setMoisTravail(saisonnier.getMoisTravail());

        affectationRepository.save(aff);
    }
}