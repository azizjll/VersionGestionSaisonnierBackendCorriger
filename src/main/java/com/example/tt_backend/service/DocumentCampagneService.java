package com.example.tt_backend.service;

import com.example.tt_backend.dto.DocumentCampagneDTO;
import com.example.tt_backend.entity.Campagne;
import com.example.tt_backend.entity.DocumentCampagne;
import com.example.tt_backend.repository.CampagneRepository;
import com.example.tt_backend.repository.DocumentCampagneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentCampagneService {

    private final DocumentCampagneRepository documentRepo;
    private final CampagneRepository campagneRepo;
    private final CloudinaryService cloudinaryService;

    public DocumentCampagne uploadDocument(Long campagneId,
                                           String nom,
                                           String type,
                                           MultipartFile file) throws Exception {

        // 1. vérifier campagne
        Campagne campagne = campagneRepo.findById(campagneId)
                .orElseThrow(() -> new RuntimeException("Campagne introuvable"));

        // 2. upload vers cloudinary
        String url = cloudinaryService.uploadFile(file, "campagnes");

        // 3. sauvegarde
        DocumentCampagne doc = new DocumentCampagne();
        doc.setNom(nom);
        doc.setType(type);
        doc.setUrl(url);
        doc.setCampagne(campagne);

        return documentRepo.save(doc);
    }

    public List<DocumentCampagneDTO> getDocumentsByCampagne(Long campagneId) {

        return documentRepo.findByCampagneId(campagneId)
                .stream()
                .map(doc -> {
                    DocumentCampagneDTO dto = new DocumentCampagneDTO();
                    dto.setId(doc.getId());
                    dto.setNom(doc.getNom());
                    dto.setType(doc.getType());
                    dto.setUrl(doc.getUrl());
                    dto.setCampagneId(doc.getCampagne().getId());
                    return dto;
                })
                .toList();
    }

    public void deleteDocument(Long id) {
        documentRepo.deleteById(id);
    }
}