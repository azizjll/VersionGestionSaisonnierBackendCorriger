package com.example.tt_backend.controller; // ✅ S120

import com.example.tt_backend.dto.StructureDTO;
import com.example.tt_backend.entity.StructureType;
import com.example.tt_backend.repository.StructureRepository;
import com.example.tt_backend.service.ExcelReaderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/structures")
public class StructureImportController {

    private final ExcelReaderService excelReaderService;
    private final StructureRepository structureRepository;

    public StructureImportController(ExcelReaderService excelReaderService,
                                     StructureRepository structureRepository) {
        this.excelReaderService = excelReaderService;
        this.structureRepository = structureRepository;
    }

    @GetMapping
    public List<StructureDTO> getAllStructures() {
        return structureRepository.findAll().stream()
                .map(s -> new StructureDTO(
                        s.getId(),
                        s.getNom(),
                        s.getType() == StructureType.ESPACE_COMMERCIAL ? "EC" : "CT",
                        s.getRegion() != null ? s.getRegion().getNom() : "",
                        s.getAdresse() != null ? s.getAdresse() : "",
                        s.getAutorisesJuillet(), s.getRecrutesJuillet(),
                        s.getAutorisesAout(), s.getRecrutesAout()))
                .toList();
    }

    @PostMapping("/import-excel")
    // ✅ S1452 — ResponseEntity<Map<String, String>> au lieu de ResponseEntity<?>
    public ResponseEntity<Map<String, String>> importExcel(
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Fichier vide"));
        }
        try {
            excelReaderService.importStructures(file.getInputStream());
            return ResponseEntity.ok(Map.of("message", "Import réussi"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Erreur: " + e.getMessage()));
        }
    }
    // ✅ S125 — bloc de code commenté supprimé
}