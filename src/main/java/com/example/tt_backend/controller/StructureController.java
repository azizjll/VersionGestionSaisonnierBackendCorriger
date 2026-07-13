package com.example.tt_backend.controller; // ✅ S120


import com.example.tt_backend.dto.StructurePubliqueDTO;
import com.example.tt_backend.exception.CampagneInvalideException;

import com.example.tt_backend.dto.StructureDTO;
import com.example.tt_backend.entity.Structure;
import com.example.tt_backend.repository.StructureRepository;
import com.example.tt_backend.service.StructureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/structures")
public class StructureController {

    // ✅ S106 — Logger
    private static final Logger logger = LoggerFactory.getLogger(StructureController.class);

    private final StructureRepository structureRepository;
    private final StructureService structureService;

    public StructureController(StructureRepository structureRepository,
                               StructureService structureService) {
        this.structureRepository = structureRepository;
        this.structureService = structureService;
    }

    @GetMapping("/region/{regionId}")
    public List<StructureDTO> getStructuresByRegion(
            @PathVariable Long regionId,
            @RequestParam(required = false) Long campagneId) {

        List<Structure> structures = campagneId != null
                ? structureRepository.findByRegionIdAndCampagneId(regionId, campagneId)
                : structureRepository.findByRegionId(regionId);

        return structures.stream()
                .map(s -> new StructureDTO(
                        s.getId(), s.getNom(), s.getType().name(),
                        s.getRegion().getNom(), s.getAdresse(),
                        s.getAutorisesJuillet(), s.getRecrutesJuillet(),
                        s.getAutorisesAout(), s.getRecrutesAout()))
                .toList();
    }

    @PutMapping("/{id}")
    // ✅ S1452 — ResponseEntity<String> au lieu de ResponseEntity<?>
    // ✅ S112 — NoSuchElementException au lieu de RuntimeException
    public ResponseEntity<String> updateStructure(
            @PathVariable Long id,
            @RequestBody StructureDTO dto) {

        Structure s = structureRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Structure non trouvée"));

        s.setNom(dto.getNom());
        s.setAdresse(dto.getAdresse());
        s.setAutorisesJuillet(dto.getAutorisesJuillet());
        s.setAutorisesAout(dto.getAutorisesAout());
        structureRepository.save(s);
        return ResponseEntity.ok("Structure mise à jour");
    }

    @GetMapping("/campagne-active")
    public ResponseEntity<List<StructureDTO>> getStructuresCampagneActive() {
        try {
            List<StructureDTO> structures = structureService.getStructuresCampagneActive()
                    .stream()
                    .map(s -> new StructureDTO(
                            s.getId(), s.getNom(), s.getType().name(),
                            s.getRegion().getNom(), s.getAdresse(),
                            s.getAutorisesJuillet(), s.getRecrutesJuillet(),
                            s.getAutorisesAout(), s.getRecrutesAout()))
                    .toList();
            return ResponseEntity.ok(structures);
        } catch (Exception e) {
            logger.error("=== Erreur structures : {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }


    @GetMapping("/campagne/{code}/publique")
    public ResponseEntity<List<StructurePubliqueDTO>> getStructuresParCodeCampagne(
            @PathVariable String code) {
        try {
            return ResponseEntity.ok(structureService.getStructuresParCodeCampagne(code));
        } catch (CampagneInvalideException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }


    @GetMapping("/campagne-active/publique")
    public ResponseEntity<List<StructureDTO>> getStructuresCampagneActivePublique() {
        try {
            List<StructureDTO> structures = structureService
                    .getStructuresCampagneActivePublique()
                    .stream()
                    .map(s -> new StructureDTO(
                            s.getId(), s.getNom(), s.getType().name(),
                            s.getRegion().getNom(), s.getAdresse(),
                            s.getAutorisesJuillet(), s.getRecrutesJuillet(),
                            s.getAutorisesAout(), s.getRecrutesAout()))
                    .toList();
            return ResponseEntity.ok(structures);
        } catch (Exception e) {
            logger.error("=== Erreur structures publiques : {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
