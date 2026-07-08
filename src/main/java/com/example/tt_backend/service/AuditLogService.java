package com.example.tt_backend.service;

import com.example.tt_backend.entity.AuditLog;
import com.example.tt_backend.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    // ✅ S107 — Regrouper les paramètres dans un record
    @Builder
    public record AuditLogRequest(
            String email,
            String action,
            String entite,
            Long entiteId,
            Object avant,
            Object apres,
            String ip,
            String statut
    ) {}

    @Async
    // ✅ S107 — 1 paramètre au lieu de 8
    public void log(AuditLogRequest req) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUtilisateurEmail(req.email());
            auditLog.setAction(req.action());
            auditLog.setEntite(req.entite());
            auditLog.setEntiteId(req.entiteId());
            auditLog.setAdresseIp(req.ip());
            auditLog.setStatut(req.statut());

            if (req.avant() != null)
                auditLog.setDonneesAvant(objectMapper.writeValueAsString(req.avant()));
            if (req.apres() != null)
                auditLog.setDonneesApres(objectMapper.writeValueAsString(req.apres()));

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement du log d'audit", e);
        }
    }

    public List<AuditLog> getTousLesLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }

    public List<AuditLog> getLogsParUtilisateur(String email) {
        return auditLogRepository.findByUtilisateurEmailOrderByTimestampDesc(email);
    }

    public List<AuditLog> getLogsParEntite(String entite, Long id) {
        return auditLogRepository.findByEntiteAndEntiteIdOrderByTimestampDesc(entite, id);
    }
}