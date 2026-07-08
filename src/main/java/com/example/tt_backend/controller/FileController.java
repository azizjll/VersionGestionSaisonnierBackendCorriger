package com.example.tt_backend.controller; // ✅ S120

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;         // ✅ S112 — import exception spécifique
import java.net.MalformedURLException; // ✅ S112 — import exception spécifique
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/files")
public class FileController {

    @Value("${storage.local.path}")
    private String storagePath;

    @GetMapping("/{folder}/{subfolder}/{filename}")
    // ✅ S112 — throws Exception → throws IOException, MalformedURLException
    public ResponseEntity<Resource> serveFile(
            @PathVariable String folder,
            @PathVariable String subfolder,
            @PathVariable String filename) throws IOException, MalformedURLException {

        Path filePath = Paths.get(storagePath, folder, subfolder, filename).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/{folder}/{filename}")
    // ✅ S112 — throws Exception → throws IOException, MalformedURLException
    public ResponseEntity<Resource> serveFile2(
            @PathVariable String folder,
            @PathVariable String filename) throws IOException, MalformedURLException {

        Path filePath = Paths.get(storagePath, folder, filename).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}