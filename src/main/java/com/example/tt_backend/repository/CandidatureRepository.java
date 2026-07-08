package com.example.tt_backend.repository;

import com.example.tt_backend.entity.Campagne;
import com.example.tt_backend.entity.Candidature;
import com.example.tt_backend.entity.Structure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CandidatureRepository extends JpaRepository<Candidature, Long> {
    List<Candidature> findBySaisonnierRegionId(Long regionId);
    List<Candidature> findByCampagneIdAndSaisonnierRegionId(Long campagneId, Long regionId);
    List<Candidature> findBySaisonnierId(Long saisonnierIdId);
    boolean existsBySaisonnierIdAndCampagneId(Long saisonnierId, Long campagneId);

    @Query("""
    SELECT DISTINCT c FROM Candidature c
    JOIN Affectation a ON a.saisonnier = c.saisonnier
    WHERE a.structure = :structure
    AND c.campagne = :campagne
""")
    List<Candidature> findByStructureAndCampagne(
            @Param("structure") Structure structure,
            @Param("campagne") Campagne campagne);
}
