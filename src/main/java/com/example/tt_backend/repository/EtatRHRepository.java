package com.example.tt_backend.repository;

import com.example.tt_backend.entity.EtatRH;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EtatRHRepository extends JpaRepository<EtatRH, Long> {

    // Pour RH_REGIONAL : son propre état sur la campagne active
    Optional<EtatRH> findByUtilisateurIdAndCampagneId(Long utilisateurId, Long campagneId);

    // Pour ADMIN : tous les états de la campagne active
    List<EtatRH> findByCampagneId(Long campagneId);


}