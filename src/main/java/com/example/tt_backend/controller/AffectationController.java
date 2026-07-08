package com.example.tt_backend.controller;

import com.example.tt_backend.entity.Structure;
import com.example.tt_backend.repository.StructureRepository;
import com.example.tt_backend.service.AffectationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/affectations")
public class AffectationController {

    // ✅ S6813 — Injection par constructeur
    private final AffectationService affectationService;
    private final StructureRepository structureRepository;

    public AffectationController(AffectationService affectationService,
                                 StructureRepository structureRepository) {
        this.affectationService = affectationService;
        this.structureRepository = structureRepository;
    }
    @PostMapping("/assign")
    // ✅ S1452 — Remplacer ResponseEntity<?> par ResponseEntity<Map<String, String>>
    public ResponseEntity<Map<String, String>> affecter(
            @RequestParam Long saisonnierId,
            @RequestParam Long structureId,
            @RequestParam Long campagneId) {

        affectationService.affecterSaisonnier(saisonnierId, structureId, campagneId);

        return ResponseEntity.ok(
                Map.of("message", "Affectation réalisée avec succès")
        );
    }

    @GetMapping("/region/{regionId}")
    public List<Structure> getByRegion(@PathVariable Long regionId){
        return structureRepository.findByRegionId(regionId);
    }
}