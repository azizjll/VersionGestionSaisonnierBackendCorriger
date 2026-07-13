package com.example.tt_backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Slf4j
public class CloudinaryService {

    @Value("${storage.local.path}")
    private String storagePath;

    @Value("${storage.local.base-url}")
    private String baseUrl;

    public String uploadFile(MultipartFile file, String folder) {
        try {
            // Créer le dossier s'il n'existe pas
            Path dirPath = Paths.get(storagePath, folder);
            Files.createDirectories(dirPath);

            // Nom unique pour éviter les conflits
            String extension = getExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID().toString() + extension;

            // Sauvegarder le fichier
            Path filePath = dirPath.resolve(fileName);
            Files.write(filePath, file.getBytes());

            // Retourner l'URL d'accès
            return baseUrl + "/" + folder + "/" + fileName;

        } catch (IOException e) {
            throw new IllegalStateException("Erreur stockage local : " + e.getMessage(), e);
        }
    }

    /**
     * 🆕 Supprime un fichier stocké localement à partir de son URL publique.
     * Ex. url = baseUrl + "/candidatures/12/abc-def.pdf"
     */
    public void deleteFile(String url) {
        if (url == null || url.isBlank()) {
            return;
        }

        try {
            String relativePath = extraireCheminRelatif(url);
            if (relativePath == null) {
                log.warn("Impossible d'extraire le chemin relatif depuis l'URL : {}", url);
                return;
            }

            Path filePath = Paths.get(storagePath, relativePath);
            boolean deleted = Files.deleteIfExists(filePath);

            if (!deleted) {
                log.warn("Fichier introuvable lors de la suppression : {}", filePath);
            }

        } catch (IOException e) {
            throw new IllegalStateException("Erreur suppression fichier local : " + e.getMessage(), e);
        }
    }

    private String extraireCheminRelatif(String url) {
        String prefix = baseUrl + "/";
        if (!url.startsWith(prefix)) {
            return null;
        }
        // ex: "candidatures/12/abc-def.pdf"
        return url.substring(prefix.length());
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return "." + filename.substring(filename.lastIndexOf('.') + 1);
    }
}