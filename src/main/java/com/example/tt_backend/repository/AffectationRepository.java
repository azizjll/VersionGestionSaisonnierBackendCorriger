package com.example.tt_backend.repository;

import com.example.tt_backend.entity.Affectation;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;

public interface AffectationRepository extends JpaRepository<Affectation, Long> {
    long countByStructureIdAndCampagneId(Long structureId, Long campagneId);


    Optional<Affectation> findTopBySaisonnierIdOrderByDateAffectationDesc(Long saisonnierId);



}