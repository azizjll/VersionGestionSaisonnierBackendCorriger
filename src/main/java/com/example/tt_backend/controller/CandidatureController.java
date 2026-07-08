package com.example.tt_backend.controller; // ✅ S120

import com.example.tt_backend.dto.DemandeAutorisationDTO;
import com.example.tt_backend.entity.Candidature;
import com.example.tt_backend.entity.Document;
import com.example.tt_backend.service.CandidatureService;
import com.example.tt_backend.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/candidatures")
public class CandidatureController {

    // ✅ S1192 — Constantes pour les literals dupliqués
    private static final String MESSAGE_KEY        = "message";
    private static final String UNAUTHENTICATED    = "Non authentifié";
    private static final String SUCCESS_KEY        = "success";

    private final CandidatureService candidatureService;
    private final JwtUtils jwtUtils;

    public CandidatureController(CandidatureService candidatureService, JwtUtils jwtUtils) {
        this.candidatureService = candidatureService;
        this.jwtUtils = jwtUtils;
    }

    // =========================
    // UTILITAIRE
    // =========================
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip != null && !ip.isEmpty()) ? ip.split(",")[0] : request.getRemoteAddr();
    }

    private String getCurrentUserEmail() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        // ✅ S112 — IllegalStateException au lieu de RuntimeException
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException(UNAUTHENTICATED);
        }
        return auth.getName();
    }

    // =========================
    // DEPOT
    // =========================
    @PostMapping(value = "/depot", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    // ✅ S1452 — ResponseEntity<Map<String, Object>> au lieu de ResponseEntity<?>
    public ResponseEntity<Map<String, Object>> deposerCandidature(
            @RequestParam String nom,
            @RequestParam String prenom,
            @RequestParam String cin,
            @RequestParam String rib,
            @RequestParam String telephone,
            @RequestParam String email,
            @RequestParam String nomPrenomParent,
            @RequestParam String matriculeParent,
            @RequestParam String niveauEtude,
            @RequestParam String diplomeNom,
            @RequestParam String specialiteDiplome,
            @RequestParam String moisTravail,
            @RequestParam Long regionId,
            @RequestParam Long campagneId,
            @RequestParam Long structureId,
            @RequestParam("cinFile") MultipartFile cinFile,
            @RequestParam("diplome") MultipartFile diplome,
            @RequestParam("ribFile") MultipartFile ribFile,
            @RequestParam(defaultValue = "false") boolean demandeAdminAutorisee,
            @RequestParam(required = false, defaultValue = "") String messageDemandeAdmin,
            Authentication authentication
    ) {
        try {
            String rhEmail = authentication != null ? authentication.getName() : "";
            CandidatureService.DeposerCandidatureRequest req =
                    new CandidatureService.DeposerCandidatureRequest();

            req.nom = nom.trim();
            req.prenom = prenom.trim();
            req.cin = cin.trim();
            req.rib = rib.trim();
            req.telephone = telephone.trim();
            req.email = email.trim();
            req.nomPrenomParent = nomPrenomParent.trim();
            req.matriculeParent = matriculeParent.trim();
            req.niveauEtude = niveauEtude.trim();
            req.diplomeNom = diplomeNom.trim();
            req.specialiteDiplome = specialiteDiplome.trim();
            req.moisTravail = moisTravail.trim();
            req.regionId = regionId;
            req.campagneId = campagneId;
            req.structureId = structureId;
            req.cinFile = cinFile;
            req.diplome = diplome;
            req.ribFile = ribFile;
            req.demandeAdminAutorisee = demandeAdminAutorisee;
            req.messageDemandeAdmin = messageDemandeAdmin;
            req.rhEmail = rhEmail;

            candidatureService.deposerCandidature(req);
            return ResponseEntity.ok(Map.of(MESSAGE_KEY,
                    "Votre candidature a été envoyée avec succès. Un email contenant vos identifiants (email et mot de passe) vous a été envoyé. Veuillez consulter votre boîte mail pour accéder à votre compte."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of(SUCCESS_KEY, false, MESSAGE_KEY, e.getMessage())
            );
	} catch (Exception e) {
    	    return ResponseEntity.status(500).body(
        	    Map.of(SUCCESS_KEY, false, MESSAGE_KEY, "" +  e.getMessage()));
        }
    }

    // =========================
    // READ
    // =========================
    @GetMapping("/mes-candidatures")
    public ResponseEntity<List<Candidature>> getCandidaturesByRegion(
            @RequestParam Long regionId,
            HttpServletRequest request) {
        String email = getCurrentUserEmail();
        return ResponseEntity.ok(
                candidatureService.getCandidaturesByRegion(regionId, email, getClientIp(request)));
    }

    @GetMapping("/filtrer")
    public ResponseEntity<List<Candidature>> getCandidaturesByCampagneAndRegion(
            @RequestParam Long campagneId,
            @RequestParam Long regionId,
            HttpServletRequest request) {
        String email = getCurrentUserEmail();
        return ResponseEntity.ok(
                candidatureService.getCandidaturesByCampagneAndRegion(campagneId, regionId, email, getClientIp(request)));
    }

    @GetMapping("/filtrer/count")
    public ResponseEntity<Long> countSaisonnierByCampagneAndRegion(
            @RequestParam Long campagneId,
            @RequestParam Long regionId,
            HttpServletRequest request) {
        String email = getCurrentUserEmail();
        long count = candidatureService.getCandidaturesByCampagneAndRegion(campagneId, regionId, email, getClientIp(request))
                .stream()
                .map(c -> c.getSaisonnier().getId())
                .distinct()
                .count();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Candidature>> getAllCandidatures(HttpServletRequest request) {
        String email = getCurrentUserEmail();
        return ResponseEntity.ok(
                candidatureService.getAllCandidatures(email, getClientIp(request)));
    }

    @GetMapping("/documents")
    public ResponseEntity<List<Document>> getDocumentsBySaisonnier(
            @RequestParam Long saisonnierId,
            HttpServletRequest request) {
        String email = getCurrentUserEmail();
        return ResponseEntity.ok(
                candidatureService.getDocumentsBySaisonnier(saisonnierId, email, getClientIp(request)));
    }

    @GetMapping("/saisonnier/{id}")
    public ResponseEntity<Object> getSaisonnier(
            @PathVariable Long id,
            HttpServletRequest request) {
        String email = getCurrentUserEmail();
        return ResponseEntity.ok(
                candidatureService.getSaisonnierById(id, email, getClientIp(request)));
    }

    @GetMapping("/mon-historique")
    public ResponseEntity<Object> getMonHistorique(
            Authentication authentication,
            HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(UNAUTHENTICATED);
        }
        return ResponseEntity.ok(
                candidatureService.getHistoriqueCandidatures(authentication.getName(), getClientIp(request)));
    }

    @GetMapping("/mes-documents")
    public ResponseEntity<Object> getMesDocuments(
            Authentication authentication,
            HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(UNAUTHENTICATED);
        }
        return ResponseEntity.ok(
                candidatureService.getDocumentsByEmail(authentication.getName(), getClientIp(request)));
    }

    @GetMapping("/mon-profil")
    public ResponseEntity<Object> getMonProfil(
            Authentication authentication,
            HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(UNAUTHENTICATED);
        }
        return ResponseEntity.ok(
                candidatureService.getProfilByEmail(authentication.getName(), getClientIp(request)));
    }

    @GetMapping("/parent-by-matricule")
    public ResponseEntity<Map<String, Object>> getParentByMatricule(
            @RequestParam String matricule,
            HttpServletRequest request) {
        try {
            String email = getCurrentUserEmail();
            return ResponseEntity.ok(Map.of(MESSAGE_KEY,
                    candidatureService.getParentByMatricule(matricule, email, getClientIp(request))));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(MESSAGE_KEY, e.getMessage()));
        }
    }

    @GetMapping("/{id}/structure")
    public ResponseEntity<Object> getStructureByCandidature(
            @PathVariable Long id,
            HttpServletRequest request) {
        String email = getCurrentUserEmail();
        return ResponseEntity.ok(
                candidatureService.getStructureByCandidatureId(id, email, getClientIp(request)));
    }

    // =========================
    // UPDATE
    // =========================
    @PutMapping("/update/{id}")
    public ResponseEntity<Candidature> updateCandidature(
            @PathVariable Long id,
            @RequestParam String nom,
            @RequestParam String prenom,
            @RequestParam Integer cin,
            @RequestParam String rib,
            @RequestParam String telephone,
            @RequestParam String email,
            @RequestParam Long regionId,
            @RequestParam(required = false) String moisTravail,
            @RequestParam String statut,
            @RequestParam(required = false) String commentaire,
            @RequestParam(required = false) Long structureId,
            @RequestParam(required = false, defaultValue = "") String nomPrenomParent,
            @RequestParam(required = false, defaultValue = "") String matriculeParent,
            @RequestParam(required = false, defaultValue = "") String niveauEtude,
            @RequestParam(required = false, defaultValue = "") String diplome,
            @RequestParam(required = false, defaultValue = "") String specialiteDiplome,
            HttpServletRequest request) {
        String userEmail = getCurrentUserEmail();
        CandidatureService.UpdateCandidatureRequest req =
                new CandidatureService.UpdateCandidatureRequest();

        req.candidatureId = id;
        req.nom = nom;
        req.prenom = prenom;
        req.cin = cin;
        req.rib = rib;
        req.telephone = telephone;
        req.email = email;
        req.regionId = regionId;
        req.moisTravail = moisTravail;
        req.statut = statut;
        req.commentaire = commentaire;
        req.structureId = structureId;
        req.nomPrenomParent = nomPrenomParent;
        req.matriculeParent = matriculeParent;
        req.niveauEtude = niveauEtude;
        req.diplome = diplome;
        req.specialiteDiplome = specialiteDiplome;
        req.userEmail = userEmail;
        req.ip = getClientIp(request);

        return ResponseEntity.ok(
                candidatureService.updateCandidature(req)
        );
    }

    // =========================
    // LOGIQUE MÉTIER
    // =========================
    @PostMapping("/demande-autorisation")
    public ResponseEntity<Map<String, String>> demandeAutorisation(
            @RequestBody DemandeAutorisationDTO dto,
            HttpServletRequest request) {
        String email = getCurrentUserEmail();
        candidatureService.envoyerDemandeJuilletAout(
                dto.getCandidatureId(), dto.getCommentaire(), email, getClientIp(request));
        return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Email envoyé aux administrateurs"));
    }

    @PostMapping("/upload-parents")
    public ResponseEntity<Map<String, Object>> uploadParentsExcel(
            @RequestParam("fichier") MultipartFile fichier,
            @RequestParam Long campagneId,
            HttpServletRequest request) {
        try {
            candidatureService.uploadParentsExcel(
                    fichier, campagneId, getCurrentUserEmail(), getClientIp(request));
            return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Import réussi"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(MESSAGE_KEY, e.getMessage()));
        }
    }

    @GetMapping("/par-structure")
    public ResponseEntity<List<Candidature>> getCandidaturesParStructure(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String email = jwtUtils.getUsernameFromToken(token);
        return ResponseEntity.ok(candidatureService.getCandidaturesParStructureResponsable(email));
    }
}
