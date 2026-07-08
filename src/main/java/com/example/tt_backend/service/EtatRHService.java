package com.example.tt_backend.service;

import com.example.tt_backend.dto.EtatRHDTO;
import com.example.tt_backend.entity.*;
import com.example.tt_backend.repository.CampagneRepository;
import com.example.tt_backend.repository.EtatRHRepository;
import com.example.tt_backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtatRHService {

    private final EtatRHRepository etatRHRepository;
    private final CampagneRepository campagneRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final CloudinaryService cloudinaryService;
    private final AuditLogService auditLogService;

    private static final String ENTITE = "EtatRH";
    private static final String SUCCESS = "SUCCESS";

    // ── RH_REGIONAL : uploader son état ──────────────────────────
    public EtatRH uploadEtat(MultipartFile file, String email, String ip) {

        log.info("Email JWT : {}", email);

        Utilisateur rh = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + email));

        log.info("RH trouvé : {} | région : {}", rh.getId(), rh.getRegion());

        if (rh.getRegion() == null) {
            throw new IllegalStateException("Aucune région associée à ce compte RH");
        }

        List<Campagne> campagnes = campagneRepository.findByStatut(StatutCampagne.ACTIVE);
        log.info("Campagnes actives : {}", campagnes.size());

        if (campagnes.isEmpty()) {
            throw new IllegalStateException("Aucune campagne active trouvée");
        }

        Campagne campagne = campagnes.get(0);
        log.info("Campagne ID : {}", campagne.getId());

        String url;
        try {
            url = cloudinaryService.uploadFile(file, "etats_rh");
            log.info("URL Cloudinary : {}", url);
        } catch (Exception e) {
            throw new IllegalStateException("Erreur Cloudinary : " + e.getMessage(), e);
        }

        EtatRH etat = etatRHRepository
                .findByUtilisateurIdAndCampagneId(rh.getId(), campagne.getId())
                .orElse(new EtatRH());

        String snapshotAvant = etat.getId() != null
                ? etat.getStatut() + " | " + etat.getNomFichier()
                : null;

        etat.setUrl(url);
        etat.setNomFichier(file.getOriginalFilename());
        etat.setDateUpload(LocalDateTime.now());
        etat.setStatut(StatutEtat.SOUMIS);
        etat.setUtilisateur(rh);
        etat.setCampagne(campagne);
        etat.setRegion(rh.getRegion());

        EtatRH saved = etatRHRepository.save(etat);

        String action = snapshotAvant == null ? "UPLOAD_ETAT" : "RE_UPLOAD_ETAT";

        auditLogService.log(
                AuditLogService.AuditLogRequest.builder()
                        .email(email)
                        .action(action)
                        .entite(ENTITE)
                        .entiteId(saved.getId())
                        .avant(snapshotAvant)
                        .apres(saved)
                        .ip(ip)
                        .statut(SUCCESS)
                        .build()
        );

        return saved;
    }

    // ── RH_REGIONAL : voir son propre état ───────────────────────
    public Optional<EtatRHDTO> getMonEtat(String email, String ip) {

        Utilisateur rh = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        Campagne campagne = campagneRepository.findByStatut(StatutCampagne.ACTIVE)
                .stream().findFirst()
                .orElse(null);

        if (campagne == null) {
            auditLogService.log(
                    AuditLogService.AuditLogRequest.builder()
                            .email(email)
                            .action("READ_MON_ETAT")
                            .entite(ENTITE)
                            .entiteId(null)
                            .avant(null)
                            .apres("aucune campagne active")
                            .ip(ip)
                            .statut(SUCCESS)
                            .build()
            );
            return Optional.empty();
        }

        Optional<EtatRHDTO> result = etatRHRepository
                .findByUtilisateurIdAndCampagneId(rh.getId(), campagne.getId())
                .map(this::toDTO);

        auditLogService.log(
                AuditLogService.AuditLogRequest.builder()
                        .email(email)
                        .action("READ_MON_ETAT")
                        .entite(ENTITE)
                        .entiteId(campagne.getId())
                        .avant(null)
                        .apres(result.isPresent() ? "état trouvé" : "aucun état")
                        .ip(ip)
                        .statut(SUCCESS)
                        .build()
        );

        return result;
    }

    // ── ADMIN : tous les états de la campagne active ──────────────
    public List<EtatRHDTO> getAllEtatsCampagneActive(String email, String ip) {

        Campagne campagne = campagneRepository.findByStatut(StatutCampagne.ACTIVE)
                .stream().findFirst()
                .orElse(null);

        if (campagne == null) {
            auditLogService.log(
                    AuditLogService.AuditLogRequest.builder()
                            .email(email)
                            .action("READ_ALL_ETATS")
                            .entite(ENTITE)
                            .entiteId(null)
                            .avant(null)
                            .apres("aucune campagne active")
                            .ip(ip)
                            .statut(SUCCESS)
                            .build()
            );
            return List.of();
        }

        List<EtatRHDTO> result = etatRHRepository
                .findByCampagneId(campagne.getId())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        auditLogService.log(
                AuditLogService.AuditLogRequest.builder()
                        .email(email)
                        .action("READ_ALL_ETATS")
                        .entite(ENTITE)
                        .entiteId(campagne.getId())
                        .avant(null)
                        .apres(result.size() + " résultats")
                        .ip(ip)
                        .statut(SUCCESS)
                        .build()
        );

        return result;
    }

    // ── ADMIN : valider / rejeter ─────────────────────────────────
    public EtatRH changerStatut(Long etatId, StatutEtat statut, String email, String ip) {

        EtatRH etat = etatRHRepository.findById(etatId)
                .orElseThrow(() -> new IllegalArgumentException("État introuvable : " + etatId));

        String snapshotAvant = etat.getStatut().toString();

        etat.setStatut(statut);
        EtatRH saved = etatRHRepository.save(etat);

        auditLogService.log(
                AuditLogService.AuditLogRequest.builder()
                        .email(email)
                        .action("CHANGER_STATUT")
                        .entite(ENTITE)
                        .entiteId(etatId)
                        .avant(snapshotAvant)
                        .apres(statut)
                        .ip(ip)
                        .statut(SUCCESS)
                        .build()
        );

        return saved;
    }

    // ── Mapping ───────────────────────────────────────────────────
    private EtatRHDTO toDTO(EtatRH e) {
        return new EtatRHDTO(
                e.getId(),
                e.getUrl(),
                e.getNomFichier(),
                e.getStatut().name(),
                e.getDateUpload(),
                e.getRegion() != null ? e.getRegion().getNom() : "",
                e.getUtilisateur().getNom() + " " + e.getUtilisateur().getPrenom(),
                e.getCampagne().getId()
        );
    }
}