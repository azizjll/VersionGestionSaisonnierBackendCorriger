package com.example.tt_backend.controller;

import com.example.tt_backend.dto.DocumentCampagneDTO;
import com.example.tt_backend.entity.DocumentCampagne;
import com.example.tt_backend.service.DocumentCampagneService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/documents-campagne")
@RequiredArgsConstructor
public class DocumentCampagneController {

    private final DocumentCampagneService service;

    @PostMapping("/upload")
    public ResponseEntity<DocumentCampagne> uploadDocument(
            @RequestParam Long campagneId,
            @RequestParam String nom,
            @RequestParam String type,
            @RequestParam MultipartFile file
    ) {
        try {
            DocumentCampagne doc = service.uploadDocument(campagneId, nom, type, file);
            return ResponseEntity.ok(doc);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{campagneId}")
    public ResponseEntity<List<DocumentCampagneDTO>> getDocuments(@PathVariable Long campagneId) {
        return ResponseEntity.ok(service.getDocumentsByCampagne(campagneId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        service.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }
}