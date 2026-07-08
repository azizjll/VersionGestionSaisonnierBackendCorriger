package com.example.tt_backend.controller;

import com.example.tt_backend.entity.AuditLog;
import com.example.tt_backend.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditLog> getTousLesLogs() {
        return auditLogService.getTousLesLogs();
    }

    @GetMapping("/utilisateur/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditLog> getLogsParUtilisateur(@PathVariable String email) {
        return auditLogService.getLogsParUtilisateur(email);
    }

    @GetMapping("/entite/{entite}/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditLog> getLogsParEntite(
            @PathVariable String entite,
            @PathVariable Long id) {
        return auditLogService.getLogsParEntite(entite, id);
    }
}