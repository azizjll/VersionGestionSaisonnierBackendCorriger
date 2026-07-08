package com.example.tt_backend.controller; // ✅ S120 — package en minuscules

import org.springframework.http.ResponseEntity;

import com.example.tt_backend.dto.CampagnePubliqueDTO;
import com.example.tt_backend.exception.CampagneInvalideException;

import com.example.tt_backend.dto.CampagneRequestDTO;
import com.example.tt_backend.entity.Campagne;
import com.example.tt_backend.service.CampagneService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
// ✅ S1128 — import PreAuthorize supprimé (inutilisé)
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/campagnes")
@RequiredArgsConstructor
public class CampagneController {

    private final CampagneService campagneService;

    // ✅ S112 — Créer une exception métier spécifique
    private static final String UNAUTHENTICATED_MSG = "Utilisateur non authentifié";

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip != null && !ip.isEmpty()) ? ip.split(",")[0] : request.getRemoteAddr();
    }

    private String getCurrentUserEmail() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        // ✅ S112 — Remplacer RuntimeException par IllegalStateException (exception spécifique)
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException(UNAUTHENTICATED_MSG);
        }
        return auth.getName();
    }

    // =========================
    // CREATE
    // =========================
    @PostMapping
    public Campagne creerCampagne(
            @RequestBody CampagneRequestDTO dto,
            HttpServletRequest request) {
        return campagneService.creerCampagne(dto,
                getCurrentUserEmail(), getClientIp(request));
    }

    @PostMapping(value = "/avec-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Campagne creerCampagneAvecExcel(
            @RequestPart("campagne") CampagneRequestDTO dto,
            @RequestPart("fichier") MultipartFile fichierExcel,
            HttpServletRequest request) {
        return campagneService.creerCampagneAvecExcel(dto, fichierExcel,
                getCurrentUserEmail(), getClientIp(request));
    }

    // =========================
    // READ
    // =========================

    @GetMapping("/{code}/publique")
    public ResponseEntity<CampagnePubliqueDTO> getCampagneParCode(@PathVariable String code) {
        try {
            return ResponseEntity.ok(campagneService.getCampagneParCode(code));
        } catch (CampagneInvalideException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    public List<Campagne> getToutesCampagnes() {
        return campagneService.getToutesCampagnes();
    }

    @GetMapping("/{id}")
    public Campagne getCampagneParId(@PathVariable Long id) {
        return campagneService.getCampagneParId(id);
    }

    @GetMapping("/actives")
    public List<Campagne> getCampagnesActives() {
        return campagneService.getCampagnesActives();
    }

    // =========================
    // UPDATE
    // =========================
    @PutMapping("/{id}")
    public Campagne mettreAJourCampagne(
            @PathVariable Long id,
            @RequestBody CampagneRequestDTO dto,
            HttpServletRequest request) {
        return campagneService.mettreAJourCampagne(id, dto,
                getCurrentUserEmail(), getClientIp(request));
    }

    // =========================
    // DELETE
    // =========================
    @DeleteMapping("/{id}")
    public void supprimerCampagne(
            @PathVariable Long id,
            HttpServletRequest request) {
        campagneService.supprimerCampagne(id,
                getCurrentUserEmail(), getClientIp(request));
    }

    // =========================
    // LOGIQUE MÉTIER
    // =========================
    @PutMapping("/{id}/activer")
    public Campagne activerCampagne(
            @PathVariable Long id,
            HttpServletRequest request) {
        return campagneService.activerCampagne(id,
                getCurrentUserEmail(), getClientIp(request));
    }

    @PutMapping("/{id}/cloturer")
    public Campagne cloturerCampagne(
            @PathVariable Long id,
            HttpServletRequest request) {
        return campagneService.cloturerCampagne(id,
                getCurrentUserEmail(), getClientIp(request));
    }

    @GetMapping("/mes-campagnes")
    public List<Campagne> getMesCampagnes(
            @AuthenticationPrincipal UserDetails userDetails) {
        return campagneService.getCampagnesParCreateur(userDetails.getUsername());
    }
}
