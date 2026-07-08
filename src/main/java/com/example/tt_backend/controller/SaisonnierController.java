package com.example.tt_backend.controller;

import com.example.tt_backend.dto.SaisonnierDTO;
import com.example.tt_backend.dto.UpdatePaieRequest;
import com.example.tt_backend.service.SaisonnierService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/saisonniers")
@RequiredArgsConstructor
public class SaisonnierController {

    private final SaisonnierService service;

    // =========================
    // UTILITAIRE
    // =========================
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip != null && !ip.isEmpty()) ? ip.split(",")[0] : request.getRemoteAddr();
    }

    private String getCurrentUserEmail() {
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        // ✅ S112 — IllegalStateException au lieu de RuntimeException
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        return auth.getName();
    }

    // =========================
    // READ
    // =========================
    @GetMapping
    public ResponseEntity<List<SaisonnierDTO>> getAll(HttpServletRequest request) {
        String email = getCurrentUserEmail();
        return ResponseEntity.ok(service.findAll(email, getClientIp(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SaisonnierDTO> getById(
            @PathVariable Long id,
            HttpServletRequest request) {
        String email = getCurrentUserEmail();
        return ResponseEntity.ok(service.findById(id, email, getClientIp(request)));
    }

    @GetMapping("/by-campagne-region")
    public ResponseEntity<List<SaisonnierDTO>> getByCampagneAndRegion(
            @RequestParam Long campagneId,
            @RequestParam Long regionId,
            HttpServletRequest request) {
        String email = getCurrentUserEmail();
        return ResponseEntity.ok(service.findByCampagneAndRegion(campagneId, regionId, email, getClientIp(request)));
    }

    @GetMapping("/by-campagne-structure")
    public ResponseEntity<List<SaisonnierDTO>> getByCampagneAndStructure(
            @RequestParam Long campagneId,
            @RequestParam Long structureId,
            HttpServletRequest request) {
        String email = getCurrentUserEmail();
        return ResponseEntity.ok(service.findByCampagneAndStructure(campagneId, structureId, email, getClientIp(request)));
    }

    // =========================
    // UPDATE
    // =========================
    @PatchMapping("/{id}/absences")
    public ResponseEntity<SaisonnierDTO> updateAbsences(
            @PathVariable Long id,
            @RequestBody UpdatePaieRequest req,
            HttpServletRequest request) {
        String email = getCurrentUserEmail();
        return ResponseEntity.ok(service.updateAbsences(id, req, email, getClientIp(request)));
    }
}