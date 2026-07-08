package com.example.tt_backend.controller; // ✅ S120

import com.example.tt_backend.entity.ParentAutorise; // adapter selon le vrai type retourné
import com.example.tt_backend.service.ParentAutoriseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parents")
public class ParentAutoriseController {

    private final ParentAutoriseService parentService;

    public ParentAutoriseController(ParentAutoriseService parentService) {
        this.parentService = parentService;
    }

    @GetMapping("/by-campagne/{campagneId}")
    // ✅ S1452 — List<ParentAutorise> au lieu de ?
    public ResponseEntity<List<ParentAutorise>> getParentsByCampagne(@PathVariable Long campagneId) {
        return ResponseEntity.ok(parentService.getParentsByCampagne(campagneId));
    }

    @GetMapping
    // ✅ S1452
    public ResponseEntity<List<ParentAutorise>> getAllParents() {
        return ResponseEntity.ok(parentService.getAllParents());
    }

    @GetMapping("/{id}")
    // ✅ S1452
    public ResponseEntity<ParentAutorise> getParent(@PathVariable Long id) {
        return ResponseEntity.ok(parentService.getParentById(id));
    }

    @PostMapping
    // ✅ S1452
    public ResponseEntity<ParentAutorise> addParent(
            @RequestParam String nomPrenom,
            @RequestParam String matricule,
	    @RequestParam String email,
            @RequestParam int autorises) {
        return ResponseEntity.ok(
                parentService.addParent(nomPrenom.trim(), matricule.trim(), email.trim(), autorises));
    }

    @PutMapping("/{id}")
    // ✅ S1452
    public ResponseEntity<ParentAutorise> updateParent(
            @PathVariable Long id,
            @RequestParam String nomPrenom,
            @RequestParam String matricule,
	    @RequestParam String email,
            @RequestParam int autorises,
            @RequestParam int utilise) {
        return ResponseEntity.ok(
                parentService.updateParent(id, nomPrenom.trim(), matricule.trim(), email.trim(), autorises, utilise));
    }

    @DeleteMapping("/{id}")
    // ✅ S1452 — Map<String, String> pour le message de retour
    public ResponseEntity<Map<String, String>> deleteParent(@PathVariable Long id) {
        parentService.deleteParent(id);
        return ResponseEntity.ok(Map.of("message", "Supprimé"));
    }
}
