package com.example.tt_backend.controller; // ✅ S120 — package en minuscules

import com.example.tt_backend.dto.*;
import com.example.tt_backend.entity.Region;
import com.example.tt_backend.entity.RoleType;
import com.example.tt_backend.entity.Utilisateur;
import com.example.tt_backend.repository.RegionRepository;
import com.example.tt_backend.service.AuthService;
import com.example.tt_backend.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RegionRepository regionRepository;
    private final JwtUtils jwtUtils;

    // ✅ S1192 — Constante pour éviter la duplication du literal "message"
    private static final String MESSAGE_KEY = "message";

    @GetMapping("/regions")
    public ResponseEntity<List<RegionDTO>> getRegions() {
        return ResponseEntity.ok(
                regionRepository.findAll()
                        .stream()
                        .map(r -> new RegionDTO(r.getId(), r.getNom()))
                        .toList()
        );
    }

    @GetMapping("/roles")
    public List<String> getRoles() {
        // ✅ S6204 — Remplacer collect(Collectors.toList()) par toList()
        return Arrays.stream(RoleType.values())
                .map(Enum::name)
                .toList();
    }

    @PostMapping("/signin-admin")
    // ✅ S1452 — Remplacer ResponseEntity<?> par ResponseEntity<Map<String, String>>
    public ResponseEntity<Map<String, String>> signinAdministrateur(
            @RequestBody SigninAdministrateurRequest request) {
        String jwt = authService.signinAdministrateur(request);
        return ResponseEntity.ok(Map.of("token", jwt));
    }

    @PostMapping("/signup")
    // ✅ S1452
    public ResponseEntity<Map<String, String>> signup(@RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok(Map.of(
                MESSAGE_KEY, "Inscription réussie ! Vérifiez votre email."
        ));
    }

    @GetMapping("/verify")
    public String verify(@RequestParam String token) {
        authService.verifyToken(token);
        return "verification-success";
    }

    @PostMapping("/signin")
    // ✅ S1452
    public ResponseEntity<Map<String, String>> signin(@RequestBody SigninRequest request) {
        String jwt = authService.signin(request);
        return ResponseEntity.ok(Map.of("token", jwt));
    }

    @PostMapping("/forgot-password")
    // ✅ S1452
    public ResponseEntity<Map<String, String>> forgotPassword(
            @RequestBody PasswordResetRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(Map.of(
                MESSAGE_KEY, "Lien de réinitialisation envoyé par email."
        ));
    }

    @PostMapping("/reset-password")
    // ✅ S1452
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestBody NewPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of(
                MESSAGE_KEY, "Mot de passe réinitialisé avec succès !"
        ));
    }

    @GetMapping("/my-region")
    public ResponseEntity<RegionDTO> getMyRegion(
            @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }
        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }
        String email = jwtUtils.getUsernameFromToken(token);
        Utilisateur user = authService.findByEmail(email);
        if (user.getRegion() == null) {
            return ResponseEntity.notFound().build();
        }
        Region region = user.getRegion();
        return ResponseEntity.ok(new RegionDTO(region.getId(), region.getNom()));
    }
}