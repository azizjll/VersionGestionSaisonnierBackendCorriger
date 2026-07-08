package com.example.tt_backend.repository;

import com.example.tt_backend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUtilisateurEmailOrderByTimestampDesc(String email);


    List<AuditLog> findByEntiteAndEntiteIdOrderByTimestampDesc(String entite, Long entiteId);

    List<AuditLog> findAllByOrderByTimestampDesc();
}