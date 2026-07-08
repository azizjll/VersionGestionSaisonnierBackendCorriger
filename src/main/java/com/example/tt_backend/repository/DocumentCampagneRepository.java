package com.example.tt_backend.repository;

import com.example.tt_backend.entity.DocumentCampagne;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentCampagneRepository extends JpaRepository<DocumentCampagne, Long> {

    List<DocumentCampagne> findByCampagneId(Long campagneId);
}