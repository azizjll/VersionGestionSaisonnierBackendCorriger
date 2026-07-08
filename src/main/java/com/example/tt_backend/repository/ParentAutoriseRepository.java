package com.example.tt_backend.repository;

import com.example.tt_backend.entity.ParentAutorise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParentAutoriseRepository extends JpaRepository<ParentAutorise, Long> {

     // 🆕
    boolean existsByMatricule(String matricule);
    List<ParentAutorise> findByCampagneId(Long campagneId);  // ← AJOUTER
    boolean existsByMatriculeAndCampagneId(String matricule, Long campagneId); // ← AJOUTER
    // ParentAutoriseRepository.java
    Optional<ParentAutorise> findByMatriculeAndCampagneId(String matricule, Long campagneId);
    Optional<ParentAutorise> findByNomPrenomAndMatriculeAndCampagneId(
            String nomPrenom, String matricule, Long campagneId);
}