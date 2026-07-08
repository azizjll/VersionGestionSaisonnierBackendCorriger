package com.example.tt_backend.controller; // ✅ S120

import com.example.tt_backend.entity.Campagne;
import com.example.tt_backend.entity.RoleType;
import com.example.tt_backend.entity.StatutCampagne;
import com.example.tt_backend.entity.Utilisateur;
import com.example.tt_backend.repository.CampagneRepository;
import com.example.tt_backend.repository.UtilisateurRepository;
import com.example.tt_backend.service.ExcelImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/superadmin")
@RequiredArgsConstructor
public class SuperAdminController {

    // ✅ S1192 — Constante pour le literal "error" dupliqué 5 fois
    private static final String ERROR_KEY = "error";

    private final ExcelImportService excelImportService;
    private final UtilisateurRepository utilisateurRepository;
    private final CampagneRepository campagneRepository;

    @PostMapping("/import-users")
    // ✅ S1452 — ResponseEntity<Map<String, Object>> au lieu de ResponseEntity<?>
    public ResponseEntity<Map<String, Object>> importUsers(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "Le fichier est vide."));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "Format invalide. Veuillez envoyer un fichier Excel (.xlsx ou .xls)"));
        }

        ExcelImportService.ImportResult result = excelImportService.importFromExcel(file);

        return ResponseEntity.ok(Map.of(
                "message", "Import terminé",
                "created", result.created(),
                "skipped", result.skipped(),
                "deleted", result.deleted(),
                "errors",  result.errors()
        ));
    }

    @PostMapping("/import-responsables-structure")
    // ✅ S1452
    public ResponseEntity<Map<String, Object>> importResponsablesStructure(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "Le fichier est vide."));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "Format invalide. Veuillez envoyer un fichier Excel (.xlsx ou .xls)"));
        }

        try {
            ExcelImportService.ImportResult result =
                    excelImportService.importResponsablesStructure(file);

            return ResponseEntity.ok(Map.of(
                    "message", "Import Responsables Structure terminé",
                    "created", result.created(),
                    "skipped", result.skipped(),
                    "deleted", result.deleted(),
                    "errors",  result.errors()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, e.getMessage()));
        }
    }

    @GetMapping("/users/responsables-structure-actifs")
    // ✅ S1452
    public ResponseEntity<List<Utilisateur>> getRsActifs() {
        List<Campagne> actives = campagneRepository.findByStatut(StatutCampagne.ACTIVE);
        if (actives.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        Campagne campagneActive = actives.get(0);

        List<Utilisateur> rs = utilisateurRepository.findAll().stream()
                .filter(u -> u.getRole() == RoleType.RESPONSABLE_STRUCTURE)
                .filter(u -> u.getCampagne() != null
                        && campagneActive.getId().equals(u.getCampagne().getId()))
                .toList();

        return ResponseEntity.ok(rs);
    }

    @GetMapping("/users")
    // ✅ S1452
    public ResponseEntity<List<Utilisateur>> getAllUsers() {
        return ResponseEntity.ok(utilisateurRepository.findAll());
    }
}