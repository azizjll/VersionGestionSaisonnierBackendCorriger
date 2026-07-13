package com.example.tt_backend.service;

import com.example.tt_backend.entity.*;
import com.example.tt_backend.repository.*;
import com.example.tt_backend.util.EmailServiceImpl;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class CandidatureService {

    // ✅ S1192 — Constantes pour les literals dupliqués
    private static final String ENTITE_CANDIDATURE       = "Candidature";
    private static final String ENTITE_DOCUMENT          = "Document";
    private static final String ENTITE_SAISONNIER        = "Saisonnier";
    private static final String ENTITE_PARENT            = "ParentAutorise";
    private static final String STATUT_SUCCESS           = "SUCCESS";
    private static final String SUFFIXE_RESULTATS        = " résultats";
    private static final String UTILISATEUR_INTROUVABLE  = "Utilisateur non trouvé";
    private static final String CANDIDATURE_INTROUVABLE  = "Candidature non trouvée";
    private static final String KEY_STRUCTURE            = "structure";

    private final CandidatureRepository candidatureRepo;
    private final CampagneRepository campagneRepo;
    private final SaisonnierRepository saisonnierRepo;
    private final DocumentRepository documentRepo;
    private final CloudinaryService cloudinaryService;
    private final RegionRepository regionRepo;
    private final StructureRepository structureRepo;
    private final AffectationRepository affectationRepo;
    private final UtilisateurRepository utilisateurRepo;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenRepository verificationTokenRepo;
    private final EmailServiceImpl emailService;
    private final ParentAutoriseRepository parentRepo;
    private final AuditLogService auditLogService;

    public CandidatureService(CandidatureRepository candidatureRepo,
                              CampagneRepository campagneRepo,
                              SaisonnierRepository saisonnierRepo,
                              DocumentRepository documentRepo,
                              CloudinaryService cloudinaryService,
                              RegionRepository regionRepo,
                              StructureRepository structureRepo,
                              AffectationRepository affectationRepo,
                              UtilisateurRepository utilisateurRepo,
                              PasswordEncoder passwordEncoder,
                              VerificationTokenRepository verificationTokenRepo,
                              EmailServiceImpl emailService,
                              ParentAutoriseRepository parentRepo,
                              AuditLogService auditLogService) {
        this.candidatureRepo        = candidatureRepo;
        this.campagneRepo           = campagneRepo;
        this.saisonnierRepo         = saisonnierRepo;
        this.documentRepo           = documentRepo;
        this.cloudinaryService      = cloudinaryService;
        this.regionRepo             = regionRepo;
        this.structureRepo          = structureRepo;
        this.affectationRepo        = affectationRepo;
        this.utilisateurRepo        = utilisateurRepo;
        this.passwordEncoder        = passwordEncoder;
        this.verificationTokenRepo  = verificationTokenRepo;
        this.emailService           = emailService;
        this.parentRepo             = parentRepo;
        this.auditLogService        = auditLogService;
    }

    // ====================
    // DTO interne — S107
    // ====================

    /**
     * ✅ S107 — Regroupe les 21 paramètres de deposerCandidature en un objet.
     */
    public static class DeposerCandidatureRequest {
        public String nom;
        public String prenom;
        public String cin;
        public String rib;
        public String telephone;
        public String email;
        public String nomPrenomParent;
        public String matriculeParent;
        public String niveauEtude;
        public String diplomeNom;
        public String specialiteDiplome;
        public String moisTravail;
        public Long regionId;
        public Long campagneId;
        public Long structureId;
        public MultipartFile cinFile;
        public MultipartFile diplome;
        public MultipartFile ribFile;
        public boolean demandeAdminAutorisee;
        public String messageDemandeAdmin;
        public String rhEmail;
    }

    /**
     * ✅ S107 — Regroupe les 19 paramètres de updateCandidature en un objet.
     */
    public static class UpdateCandidatureRequest {
        public Long   candidatureId;
        public String nom;
        public String prenom;
        public Integer cin;
        public String rib;
        public String telephone;
        public String email;
        public Long   regionId;
        public String moisTravail;
        public String statut;
        public String commentaire;
        public Long   structureId;
        public String nomPrenomParent;
        public String matriculeParent;
        public String niveauEtude;
        public String diplome;
        public String specialiteDiplome;
        public String userEmail;
        public String ip;

        // 🆕 gestion des documents
        public MultipartFile[] documents;
        public List<Long> documentsToDelete;
    }

    // ====================
    // DEPOT
    // ====================
    @Transactional
    public void deposerCandidature(DeposerCandidatureRequest req) throws Exception {

        // ── 0. Vérifier parent autorisé ─────────────────────────────
        // ✅ S112 — NoSuchElementException au lieu de RuntimeException
        ParentAutorise parent = parentRepo
                .findByNomPrenomAndMatriculeAndCampagneId(
                        req.nomPrenomParent.trim(),
                        req.matriculeParent.trim(),
                        req.campagneId)
                .orElseThrow(() -> new NoSuchElementException("Parent non autorisé"));

        boolean depasse = parent.getUtilise() >= parent.getAutorises();

        if (depasse && !req.demandeAdminAutorisee) {
            throw new IllegalStateException("Quota dépassé : Vous avez atteint le nombre maximal de matricules parents autorisés.");
        }

        // ── 1. Vérifier ou créer Saisonnier ─────────────────────────
	Saisonnier s = saisonnierRepo.findByCin(req.cin).orElse(null);

        if (s == null) {
            s = buildNouveauSaisonnier(req);
        } else {
            s.setTelephone(req.telephone);
            s.setEmail(req.email);
            saisonnierRepo.save(s);
        }

        // ── 2. Empêcher double candidature même campagne ───────────
        // ✅ S112
        if (candidatureRepo.existsBySaisonnierIdAndCampagneId(s.getId(), req.campagneId)) {
            throw new IllegalStateException("Vous avez déjà postulé à cette campagne (Matricule parrain ou N° CIN déjà existant.");
        }

        // ── 3. Créer utilisateur si n'existe pas ───────────────────
        creerUtilisateurSiAbsent(req, s);

        // ── 4. Créer candidature ───────────────────────────────────
        Candidature c = new Candidature();
        c.setDateDepot(LocalDate.now());

        if (depasse) {
            c.setStatut(StatutCandidature.EN_ATTENTE_VALIDATION_ADMIN);
            c.setCommentaire(req.messageDemandeAdmin);
        } else {
            c.setStatut(StatutCandidature.EN_ATTENTE);
        }

        // ✅ S112
        c.setCampagne(campagneRepo.findById(req.campagneId)
                .orElseThrow(() -> new NoSuchElementException("Campagne non trouvée")));
        c.setSaisonnier(s);
        candidatureRepo.save(c);

        // ── 5. Vérifier quota structure ────────────────────────────
        // ✅ S112
        Structure structure = structureRepo.findById(req.structureId)
                .orElseThrow(() -> new NoSuchElementException("Structure non trouvée"));

        int quotaMois = "JUILLET".equals(req.moisTravail)
                ? structure.getAutorisesJuillet()
                : structure.getAutorisesAout();

        long nbCandidaturesMois = affectationRepo.countByStructureIdAndCampagneIdAndMoisTravail(
                req.structureId, req.campagneId, req.moisTravail);

        if (nbCandidaturesMois >= quotaMois) {
            throw new IllegalStateException(
                    "Quota atteint pour " + req.moisTravail + " (" + quotaMois + ")");
        }

        // ── 6. Affectation ─────────────────────────────────────────
        Affectation affectation = new Affectation();
        affectation.setStructure(structure);
        affectation.setCampagne(c.getCampagne());
        affectation.setSaisonnier(s);
        affectation.setCandidature(c);   // ✅ le lien qui manquait
        affectation.setMoisTravail(req.moisTravail);
        affectation.setDateAffectation(LocalDate.now());

        if ("JUILLET".equals(req.moisTravail)) {
            structure.setRecrutesJuillet(structure.getRecrutesJuillet() + 1);
        } else {
            structure.setRecrutesAout(structure.getRecrutesAout() + 1);
        }
        affectationRepo.save(affectation);

        // ── 7. Incrémenter utilise ─────────────────────────────────
        parent.setUtilise(parent.getUtilise() + 1);
        parentRepo.save(parent);

        // ── 8. Si quota dépassé → email aux admins ────────────────
        if (depasse) {
            envoyerEmailQuotaDepasse(req, s, parent);
        }

        // ── 9. Upload documents ───────────────────────────────────
        saveDoc(c, req.cinFile,  "CIN");
        saveDoc(c, req.diplome,  "DIPLOME");
        saveDoc(c, req.ribFile,  "RIB");
    }

    // ── helpers privés pour réduire la complexité de deposerCandidature ──

    private Saisonnier buildNouveauSaisonnier(DeposerCandidatureRequest req) {
        // ✅ S112
        Saisonnier s = new Saisonnier();
        s.setNom(req.nom);
        s.setPrenom(req.prenom);
        s.setCin(req.cin);
        s.setRib(req.rib);
        s.setTelephone(req.telephone);
        s.setEmail(req.email);
        s.setNomPrenomParent(req.nomPrenomParent);
        s.setMatriculeParent(req.matriculeParent);
        s.setNiveauEtude(req.niveauEtude);
        s.setDiplome(req.diplomeNom);
        s.setSpecialiteDiplome(req.specialiteDiplome);
        s.setMoisTravail(req.moisTravail);
        s.setRegion(regionRepo.findById(req.regionId)
                .orElseThrow(() -> new NoSuchElementException("Région non trouvée")));
        return saisonnierRepo.save(s);
    }

    private void creerUtilisateurSiAbsent(DeposerCandidatureRequest req, Saisonnier s) {
        if (utilisateurRepo.findByEmail(req.email).isPresent()) return;

        String motDePasseTemp = UUID.randomUUID().toString().substring(0, 8);

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(req.nom);
        utilisateur.setPrenom(req.prenom);
        utilisateur.setEmail(req.email);
        utilisateur.setTelephone(req.telephone);
        utilisateur.setRole(RoleType.SAISONNIER);
        utilisateur.setPassword(passwordEncoder.encode(motDePasseTemp));
        utilisateur.setEnabled(true);
        utilisateur.setRegion(s.getRegion());
        utilisateur.setSaisonnier(s);
        utilisateurRepo.save(utilisateur);

        VerificationToken token = new VerificationToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(utilisateur);
        token.setExpiryDate(LocalDateTime.now().plusDays(1));
        verificationTokenRepo.save(token);

        emailService.sendSaisonnierWelcomeEmail(
                req.email,
                req.prenom + " " + req.nom,
                motDePasseTemp,
                token.getToken()
        );
    }

    private void envoyerEmailQuotaDepasse(DeposerCandidatureRequest req, Saisonnier s, ParentAutorise parent) {
        List<String> emailsAdmins = utilisateurRepo.findByRole(RoleType.ADMIN)
                .stream().map(Utilisateur::getEmail).collect(Collectors.toList());

        Utilisateur rh = utilisateurRepo.findByEmail(req.rhEmail).orElse(null);
        String prenomRH    = rh != null ? rh.getPrenom() : "Inconnu";
        String nomRH       = rh != null ? rh.getNom()    : "Inconnu";
        String directionRH = s.getRegion().getNom();

        emailService.envoyerDemandeAutorisationQuotaParent(
                s.getPrenom(), s.getNom(), s.getCin().toString(),
                req.matriculeParent, req.nomPrenomParent,
                parent.getUtilise(), parent.getAutorises(),
                directionRH, prenomRH, nomRH,
                req.messageDemandeAdmin, emailsAdmins
        );
    }

    // ====================
    // READ
    // ====================
    public List<Candidature> getCandidaturesByRegion(Long regionId, String email, String ip) {
        List<Candidature> result = candidatureRepo.findBySaisonnierRegionId(regionId);
        // ✅ S1192
        auditLogService.log(AuditLogService.AuditLogRequest.builder()
                .email(email)
                .action("READ_BY_REGION")
                .entite(ENTITE_CANDIDATURE)
                .entiteId(regionId)
                .avant(null)
                .apres(result.size() + SUFFIXE_RESULTATS)
                .ip(ip)
                .statut(STATUT_SUCCESS)
                .build());
        return result;
    }

    public List<Candidature> getCandidaturesByCampagneAndRegion(Long campagneId, Long regionId, String email, String ip) {
        List<Candidature> result = candidatureRepo.findByCampagneIdAndSaisonnierRegionId(campagneId, regionId);
        auditLogService.log(AuditLogService.AuditLogRequest.builder()
                .email(email)
                .action("READ_FILTER")
                .entite(ENTITE_CANDIDATURE)
                .entiteId(campagneId)
                .avant(null)
                .apres(result.size() + SUFFIXE_RESULTATS)
                .ip(ip)
                .statut(STATUT_SUCCESS)
                .build());
        return result;
    }

    public List<Candidature> getAllCandidatures(String email, String ip) {
        List<Candidature> result = candidatureRepo.findAll();
        auditLogService.log(AuditLogService.AuditLogRequest.builder()
                .email(email)
                .action("READ_ALL")
                .entite(ENTITE_CANDIDATURE)
                .entiteId(null)
                .avant(null)
                .apres(result.size() + SUFFIXE_RESULTATS)
                .ip(ip)
                .statut(STATUT_SUCCESS)
                .build());
        return result;
    }

    public List<Document> getDocumentsBySaisonnier(Long saisonnierId, String email, String ip) {
        List<Document> docs = documentRepo.findByCandidatureSaisonnierId(saisonnierId);
        auditLogService.log(AuditLogService.AuditLogRequest.builder()
                .email(email)
                .action("READ_DOCUMENTS")
                .entite(ENTITE_DOCUMENT)
                .entiteId(saisonnierId)
                .avant(null)
                .apres(docs.size() + " documents")
                .ip(ip)
                .statut(STATUT_SUCCESS)
                .build());
        return docs;
    }

    public Saisonnier getSaisonnierById(Long id, String email, String ip) {
        // ✅ S112
        Saisonnier s = saisonnierRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Saisonnier non trouvé"));
        auditLogService.log(AuditLogService.AuditLogRequest.builder()
                .email(email)
                .action("READ")
                .entite(ENTITE_SAISONNIER)
                .entiteId(id)
                .avant(null)
                .apres(s)
                .ip(ip)
                .statut(STATUT_SUCCESS)
                .build());
        return s;
    }

    public List<Candidature> getHistoriqueCandidatures(String email, String ip) {
        // ✅ S112 + S1192
        Utilisateur utilisateur = utilisateurRepo.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException(UTILISATEUR_INTROUVABLE));

        Saisonnier saisonnier = utilisateur.getSaisonnier();
        if (saisonnier == null) {
            auditLogService.log(AuditLogService.AuditLogRequest.builder()
                    .email(email)
                    .action("READ_HISTORIQUE")
                    .entite(ENTITE_CANDIDATURE)
                    .entiteId(null)
                    .avant(null)
                    .apres("0" + SUFFIXE_RESULTATS)
                    .ip(ip)
                    .statut(STATUT_SUCCESS)
                    .build());
            return List.of();
        }

        List<Candidature> result = candidatureRepo.findBySaisonnierId(saisonnier.getId());
        auditLogService.log(AuditLogService.AuditLogRequest.builder()
                .email(email)
                .action("READ_HISTORIQUE")
                .entite(ENTITE_CANDIDATURE)
                .entiteId(null)
                .avant(null)
                .apres("0" + SUFFIXE_RESULTATS)
                .ip(ip)
                .statut(STATUT_SUCCESS)
                .build());
        return result;
    }

    public List<Document> getDocumentsByEmail(String email, String ip) {
        // ✅ S112 + S1192
        Utilisateur utilisateur = utilisateurRepo.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException(UTILISATEUR_INTROUVABLE));

        Saisonnier saisonnier = utilisateur.getSaisonnier();
        if (saisonnier == null) return List.of();

        List<Document> docs = documentRepo.findByCandidatureSaisonnierId(saisonnier.getId());
        auditLogService.log(AuditLogService.AuditLogRequest.builder()
                .email(email)
                .action("READ_MES_DOCUMENTS")
                .entite(ENTITE_DOCUMENT)
                .entiteId(saisonnier.getId())
                .avant(null)
                .apres(docs.size() + " documents")
                .ip(ip)
                .statut(STATUT_SUCCESS)
                .build());
        return docs;
    }

    public Map<String, Object> getProfilByEmail(String email, String ip) {
        // ✅ S112 + S1192
        Utilisateur utilisateur = utilisateurRepo.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException(UTILISATEUR_INTROUVABLE));

        Saisonnier s = utilisateur.getSaisonnier();
        if (s == null) throw new NoSuchElementException("Profil saisonnier non trouvé");

        Affectation derniereAffectation = affectationRepo
                .findTopBySaisonnierIdOrderByDateAffectationDesc(s.getId())
                .orElse(null);

        Map<String, Object> profil = new java.util.LinkedHashMap<>();
        profil.put("nom",               s.getNom());
        profil.put("prenom",            s.getPrenom());
        profil.put("email",             s.getEmail());
        profil.put("telephone",         s.getTelephone());
        profil.put("cin",               s.getCin());
        profil.put("rib",               s.getRib());
        profil.put("region", s.getRegion() != null
                ? Map.of("id", s.getRegion().getId(), "nom", s.getRegion().getNom())
                : null);
        profil.put("nomPrenomParent",   s.getNomPrenomParent());
        profil.put("matriculeParent",   s.getMatriculeParent());
        profil.put("niveauEtude",       s.getNiveauEtude());
        profil.put("diplome",           s.getDiplome());
        profil.put("specialiteDiplome", s.getSpecialiteDiplome());

        if (derniereAffectation != null && derniereAffectation.getStructure() != null) {
            Structure st = derniereAffectation.getStructure();
            profil.put(KEY_STRUCTURE, Map.of(
                    "id",      st.getId(),
                    "nom",     st.getNom(),
                    "type",    st.getType().toString(),
                    "adresse", st.getAdresse() != null ? st.getAdresse() : ""
            ));
        } else {
            profil.put(KEY_STRUCTURE, null);
        }

        auditLogService.log(AuditLogService.AuditLogRequest.builder()
                .email(email)
                .action("READ_PROFIL")
                .entite(ENTITE_SAISONNIER)
                .entiteId(s.getId())
                .avant(null)
                .apres("profil consulté")
                .ip(ip)
                .statut(STATUT_SUCCESS)
                .build());
        return profil;
    }

    public Map<String, Object> getParentByMatricule(String matricule, String email, String ip) {
        // ✅ S112
        Campagne campagneActive = campagneRepo.findAll().stream()
                .filter(c -> c.getStatut() == StatutCampagne.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Aucune campagne active"));

        ParentAutorise parent = parentRepo
                .findByMatriculeAndCampagneId(matricule.trim(), campagneActive.getId())
                .orElseThrow(() -> new NoSuchElementException("Matricule introuvable"));

        boolean depasse = parent.getUtilise() >= parent.getAutorises();

        auditLogService.log(AuditLogService.AuditLogRequest.builder()
                .email(email)
                .action("READ_PARENT")
                .entite(ENTITE_PARENT)
                .entiteId(null)
                .avant(null)
                .apres("matricule: " + matricule)
                .ip(ip)
                .statut(STATUT_SUCCESS)
                .build());

        return Map.of(
                "nomPrenom", parent.getNomPrenom(),
		"email",     parent.getEmail() != null ? parent.getEmail() : "",
                "autorises", parent.getAutorises(),
                "utilise",   parent.getUtilise(),
                "depasse",   depasse
        );
    }

    public Map<String, Object> getStructureByCandidatureId(Long candidatureId, String email, String ip) {
        // ✅ S112 + S1192
        Candidature c = candidatureRepo.findById(candidatureId)
                .orElseThrow(() -> new NoSuchElementException(CANDIDATURE_INTROUVABLE));

        Affectation affectation = affectationRepo
                .findByCandidatureId(candidatureId)
                .orElse(null);

        auditLogService.log(AuditLogService.AuditLogRequest.builder()
                .email(email)
                .action("READ_STRUCTURE")
                .entite(ENTITE_CANDIDATURE)
                .entiteId(candidatureId)
                .avant(null)
                .apres(KEY_STRUCTURE + " consultée")
                .ip(ip)
                .statut(STATUT_SUCCESS)
                .build());

        if (affectation == null || affectation.getStructure() == null) {
            return Map.of(KEY_STRUCTURE, "");
        }

        Structure st = affectation.getStructure();
        return Map.of(
                "id",      st.getId(),
                "nom",     st.getNom(),
                "type",    st.getType().toString(),
                "adresse", st.getAdresse() != null ? st.getAdresse() : ""
        );
    }

    // ====================
    // UPDATE
    // ====================
    @Transactional
    public Candidature updateCandidature(UpdateCandidatureRequest req) {
        // ✅ S107 — paramètres regroupés dans UpdateCandidatureRequest
        // ✅ S112 + S1192
        Candidature c = candidatureRepo.findById(req.candidatureId)
                .orElseThrow(() -> new NoSuchElementException(CANDIDATURE_INTROUVABLE));

        String snapshotAvant = c.getStatut() + " | " + c.getSaisonnier().getEmail();

        Saisonnier s = c.getSaisonnier();
        updateSaisonnier(s, req);

        StatutCandidature ancienStatut  = c.getStatut();
        StatutCandidature nouveauStatut = StatutCandidature.valueOf(req.statut);

        c.setStatut(nouveauStatut);
        c.setCommentaire(req.commentaire);

        envoyerEmailChangementStatut(ancienStatut, nouveauStatut, s);
        gererChangementStructure(req, c, s);

        // 🆕 Gestion des documents (suppression + ajout)
        gererDocumentsSuppression(c, req.documentsToDelete);
        gererDocumentsAjout(c, req.documents);

        Candidature saved = candidatureRepo.save(c);

        auditLogService.log(AuditLogService.AuditLogRequest.builder()
                .email(req.userEmail)
                .action("UPDATE")
                .entite(ENTITE_CANDIDATURE)
                .entiteId(req.candidatureId)
                .avant(snapshotAvant)
                .apres(saved)
                .ip(req.ip)
                .statut(STATUT_SUCCESS)
                .build());

        return saved;
    }
    // ── helpers privés pour la gestion des documents (update) ──

    private void gererDocumentsSuppression(Candidature c, List<Long> documentsToDelete) {
        log.info("🔍 documentsToDelete reçu = {}", documentsToDelete);
        if (documentsToDelete == null || documentsToDelete.isEmpty()) return;

        for (Long docId : documentsToDelete) {
            Document doc = documentRepo.findById(docId).orElse(null);

            if (doc == null) {
                log.warn("Document introuvable en base : docId={}", docId);
                continue;
            }

            if (doc.getCandidature() == null || !doc.getCandidature().getId().equals(c.getId())) {
                log.warn("Document {} n'appartient pas à la candidature {}", docId, c.getId());
                continue;
            }

            try {
                cloudinaryService.deleteFile(doc.getUrl());
            } catch (Exception e) {
                log.error("Échec suppression fichier local (docId={}) : {}", docId, e.getMessage());
            }

            // 🔑 étape critique : retirer de la collection EN MÉMOIRE du parent
            if (c.getDocuments() != null) {
                c.getDocuments().removeIf(d -> d.getId().equals(docId));
            }

            // 🔑 rompre le lien avant suppression (évite les soucis de cascade inverse)
            doc.setCandidature(null);
            documentRepo.delete(doc);
        }

        // 🔑 forcer l'écriture immédiate de la suppression avant le save(c) qui suit
        documentRepo.flush();
    }

    private void gererDocumentsAjout(Candidature c, MultipartFile[] documents) {
        if (documents == null || documents.length == 0) return;

        for (MultipartFile file : documents) {
            if (file == null || file.isEmpty()) continue;

            try {
                saveDoc(c, file, "AUTRE");
            } catch (Exception e) {
                log.error("Échec upload document (candidatureId={}, fichier={}) : {}",
                        c.getId(), file.getOriginalFilename(), e.getMessage());
                throw new IllegalStateException(
                        "Erreur lors de l'upload du fichier : " + file.getOriginalFilename(), e);
            }
        }
    }

    // ── helpers privés pour updateCandidature (réduit S3776 / S6541) ──

    private void updateSaisonnier(Saisonnier s, UpdateCandidatureRequest req) {
        s.setNom(req.nom);
        s.setPrenom(req.prenom);
        s.setCin(String.valueOf(req.cin));
        s.setRib(req.rib);
        s.setTelephone(req.telephone);
        s.setEmail(req.email);
        // ✅ S112
        s.setRegion(regionRepo.findById(req.regionId)
                .orElseThrow(() -> new NoSuchElementException("Région non trouvée")));

        if (req.moisTravail     != null && !req.moisTravail.isBlank())     s.setMoisTravail(req.moisTravail);
        if (req.nomPrenomParent != null && !req.nomPrenomParent.isBlank()) s.setNomPrenomParent(req.nomPrenomParent);
        if (req.matriculeParent != null && !req.matriculeParent.isBlank()) s.setMatriculeParent(req.matriculeParent);
        if (req.niveauEtude     != null && !req.niveauEtude.isBlank())     s.setNiveauEtude(req.niveauEtude);
        if (req.diplome         != null && !req.diplome.isBlank())         s.setDiplome(req.diplome);
        if (req.specialiteDiplome != null && !req.specialiteDiplome.isBlank()) s.setSpecialiteDiplome(req.specialiteDiplome);

        saisonnierRepo.save(s);
    }

    private void envoyerEmailChangementStatut(StatutCandidature ancien, StatutCandidature nouveau, Saisonnier s) {
        if (ancien == nouveau) return;
        String emailS    = s.getEmail();
        String prenomNom = s.getPrenom() + " " + s.getNom();
        try {
            if (nouveau == StatutCandidature.ACCEPTEE) {
                emailService.sendCandidatureAccepteeEmail(emailS, prenomNom);
            } else if (nouveau == StatutCandidature.REJETEE) {
                emailService.sendCandidatureRefuseeEmail(emailS, prenomNom);
            }
        } catch (Exception e) {
            log.error("Échec envoi email statut candidature : {}", e.getMessage());
        }
    }

    private void gererChangementStructure(UpdateCandidatureRequest req, Candidature c, Saisonnier s) {
        if (req.structureId == null) return;

        // ✅ chercher par candidature, pas par saisonnier — sinon on touche
        // l'affectation d'une AUTRE campagne du même saisonnier
        Affectation ancienne = affectationRepo
                .findByCandidatureId(c.getId())
                .orElse(null);

        if (ancienne != null) {
            decrementerQuotaMois(ancienne.getStructure(), ancienne.getMoisTravail());
            affectationRepo.delete(ancienne);
        }

        Structure newStructure = structureRepo.findById(req.structureId)
                .orElseThrow(() -> new NoSuchElementException("Structure non trouvée"));

        String mois = s.getMoisTravail();
        int quotaMois = "JUILLET".equals(mois) ? newStructure.getAutorisesJuillet() : newStructure.getAutorisesAout();

        long nb = affectationRepo.countByStructureIdAndCampagneIdAndMoisTravail(req.structureId, c.getCampagne().getId(), mois);
        if (nb >= quotaMois) {
            throw new IllegalStateException("Quota atteint pour " + mois);
        }

        Affectation newAffectation = new Affectation();
        newAffectation.setStructure(newStructure);
        newAffectation.setCampagne(c.getCampagne());
        newAffectation.setSaisonnier(s);
        newAffectation.setCandidature(c);   // ✅ le lien qui manquait
        newAffectation.setMoisTravail(mois);
        newAffectation.setDateAffectation(LocalDate.now());
        incrementerQuotaMois(newStructure, mois);
        affectationRepo.save(newAffectation);
    }

    private void decrementerQuotaMois(Structure structure, String mois) {
        if ("JUILLET".equals(mois)) structure.setRecrutesJuillet(Math.max(0, structure.getRecrutesJuillet() - 1));
        else                        structure.setRecrutesAout(Math.max(0, structure.getRecrutesAout() - 1));
    }

    private void incrementerQuotaMois(Structure structure, String mois) {
        if ("JUILLET".equals(mois)) structure.setRecrutesJuillet(structure.getRecrutesJuillet() + 1);
        else                        structure.setRecrutesAout(structure.getRecrutesAout() + 1);
    }

    // ====================
    // LOGIQUE MÉTIER
    // ====================
    public void envoyerDemandeJuilletAout(Long candidatureId, String commentaire, String email, String ip) {
        // ✅ S112 + S1192
        Candidature c = candidatureRepo.findById(candidatureId)
                .orElseThrow(() -> new NoSuchElementException(CANDIDATURE_INTROUVABLE));

        Saisonnier s        = c.getSaisonnier();
        String ancienStatut = c.getStatut().toString();

        c.setStatut(StatutCandidature.EN_ATTENTE_VALIDATION_ADMIN);
        c.setCommentaire(commentaire);
        candidatureRepo.save(c);

        List<String> emailsAdmins = utilisateurRepo.findByRole(RoleType.ADMIN)
                .stream().map(Utilisateur::getEmail).collect(Collectors.toList());

        // ✅ S112
        if (emailsAdmins.isEmpty()) {
            throw new NoSuchElementException("Aucun administrateur trouvé");
        }

        emailService.envoyerDemandeAutorisationJuilletAout(
                s.getPrenom(), s.getNom(), s.getCin().toString(),
                s.getRegion().getNom(), commentaire, emailsAdmins
        );

        auditLogService.log(AuditLogService.AuditLogRequest.builder()
                .email(email)
                .action("DEMANDE_AUTORISATION")
                .entite(ENTITE_CANDIDATURE)
                .entiteId(candidatureId)
                .avant(ancienStatut)
                .apres(StatutCandidature.EN_ATTENTE_VALIDATION_ADMIN)
                .ip(ip)
                .statut(STATUT_SUCCESS)
                .build());
    }

    public void uploadParentsExcel(MultipartFile file, Long campagneId, String email, String ip) throws Exception {
        // ✅ S112
        Campagne campagne = campagneRepo.findById(campagneId)
                .orElseThrow(() -> new NoSuchElementException("Campagne introuvable"));

        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        Sheet sheet       = workbook.getSheetAt(0);
        int nbInseres     = 0;

        for (Row row : sheet) {
            // ✅ S135 — une seule condition de saut au lieu de continue/break multiples
            if (row.getRowNum() == 0) continue;

            String matricule    = getCellValueAsString(row.getCell(1)).trim();
            String nomPrenom    = getCellValueAsString(row.getCell(2)).trim();
	    String emailParent  = getCellValueAsString(row.getCell(3)).trim();
            String autorisesStr = getCellValueAsString(row.getCell(4)).trim();

            if (matricule.isEmpty() || nomPrenom.isEmpty() || autorisesStr.isEmpty()) continue;

            int autorises;
            try {
                autorises = Integer.parseInt(autorisesStr);
            } catch (NumberFormatException e) {
                log.warn("Valeur 'autorises' non numérique ignorée : {}", autorisesStr);
                continue;
            }

            if (!parentRepo.existsByMatriculeAndCampagneId(matricule, campagneId)) {
                ParentAutorise parent = new ParentAutorise();
                parent.setNomPrenom(nomPrenom);
                parent.setMatricule(matricule);
		parent.setEmail(emailParent);
                parent.setAutorises(autorises);
                parent.setUtilise(0);
                parent.setCampagne(campagne);
                parentRepo.save(parent);
                nbInseres++;
            }
        }

        workbook.close();

        auditLogService.log(AuditLogService.AuditLogRequest.builder()
                .email(email)
                .action("UPLOAD_PARENTS_EXCEL")
                .entite(ENTITE_PARENT)
                .entiteId(campagneId)
                .avant(null)
                .apres(nbInseres + " parents insérés — fichier: " + file.getOriginalFilename())
                .ip(ip)
                .statut(STATUT_SUCCESS)
                .build());
    }

    public List<Candidature> getCandidaturesParStructureResponsable(String email) {
        // ✅ S112
        List<Utilisateur> users = utilisateurRepo.findAllByEmail(email);

        Utilisateur responsable;
        if (users.size() > 1) {
            responsable = users.stream()
                    .filter(u -> u.getCampagne() != null && u.getCampagne().getStatut() == StatutCampagne.ACTIVE)
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException("Aucune campagne active."));
        } else if (users.size() == 1) {
            responsable = users.get(0);
        } else {
            throw new NoSuchElementException("Utilisateur non trouvé.");
        }

        Structure structure = responsable.getStructure();
        Campagne  campagne  = responsable.getCampagne();

        if (structure == null) throw new IllegalStateException("Aucune structure affectée.");
        if (campagne  == null) throw new IllegalStateException("Aucune campagne affectée.");

        return candidatureRepo.findByStructureAndCampagne(structure, campagne);
    }

    // ====================
    // PRIVÉ
    // ====================
    private void saveDoc(Candidature c, MultipartFile file, String type) throws Exception {
        String url = cloudinaryService.uploadFile(file, "candidatures/" + c.getId());

        Document d = new Document();
        d.setNomFichier(file.getOriginalFilename());
        d.setType(type);
        d.setUrl(url);
        d.setCandidature(c);

        documentRepo.save(d);
    }

    private String getCellValueAsString(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default      -> "";
        };
    }
}
