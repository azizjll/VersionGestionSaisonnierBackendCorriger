package com.example.tt_backend.controller; // ✅ S120

import com.example.tt_backend.dto.EtatRHDTO;
import com.example.tt_backend.entity.EtatRH;
import com.example.tt_backend.entity.StatutEtat;
import com.example.tt_backend.service.EtatRHService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EtatRHController {

    // ✅ S106 — Logger au lieu de System.out
    private static final Logger logger = LoggerFactory.getLogger(EtatRHController.class);

    private final EtatRHService etatRHService;

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip != null && !ip.isEmpty()) ? ip.split(",")[0] : request.getRemoteAddr();
    }

    private String getCurrentUserEmail() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        // ✅ S112 — IllegalStateException au lieu de RuntimeException
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        return auth.getName();
    }

    // =========================
    // RH_REGIONAL
    // =========================
    // ✅ S1452 — ResponseEntity<EtatRHDTO> au lieu de ResponseEntity<?>

    @PostMapping("/rh/etat/upload")
    public ResponseEntity<EtatRH> uploadEtat(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        try {
            logger.info("=== Upload reçu : {}", file.getOriginalFilename());
            logger.info("=== Taille : {}", file.getSize());
            logger.info("=== Content-Type : {}", file.getContentType());
            String email = getCurrentUserEmail();
            return ResponseEntity.ok(etatRHService.uploadEtat(file, email, getClientIp(request)));
        } catch (Exception e) {
            logger.error("=== ERREUR uploadEtat : {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    @GetMapping("/rh/etat/mon-etat")
    // ✅ S1452 — ResponseEntity<EtatRHDTO> au lieu de ResponseEntity<?>
    public ResponseEntity<EtatRHDTO> getMonEtat(HttpServletRequest request) {
        String email = getCurrentUserEmail();
        return ResponseEntity.ok(
                etatRHService.getMonEtat(email, getClientIp(request)).orElse(null));
    }

    // =========================
    // ADMIN
    // =========================
    @GetMapping("/admin/etats")
    public ResponseEntity<List<EtatRHDTO>> getAllEtats(HttpServletRequest request) {
        String email = getCurrentUserEmail();
        return ResponseEntity.ok(
                etatRHService.getAllEtatsCampagneActive(email, getClientIp(request)));
    }


    // ✅ S1452 — ResponseEntity<EtatRHDTO> au lieu de ResponseEntity<?>
    @PatchMapping("/admin/etats/{id}/statut")
    public ResponseEntity<EtatRH> changerStatut(
            @PathVariable Long id,
            @RequestParam StatutEtat statut,
            HttpServletRequest request) {
        String email = getCurrentUserEmail();
        return ResponseEntity.ok(
                etatRHService.changerStatut(id, statut, email, getClientIp(request)));
    }
}