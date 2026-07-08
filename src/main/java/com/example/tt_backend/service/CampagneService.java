package com.example.tt_backend.service;

import java.time.LocalDate;


import com.example.tt_backend.exception.CampagneInvalideException;
import com.example.tt_backend.dto.CampagnePubliqueDTO;
import com.example.tt_backend.dto.CampagneRequestDTO;
import com.example.tt_backend.entity.*;
import com.example.tt_backend.repository.CampagneRepository;
import com.example.tt_backend.repository.RegionRepository;
import com.example.tt_backend.repository.StructureRepository;
import com.example.tt_backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.NoSuchElementException;

// ✅ S106 — @Slf4j pour le logger
@Slf4j
@Service
@RequiredArgsConstructor
public class CampagneService {

    // ✅ S1192 — Constantes pour les literals dupliqués
    private static final String ENTITE_CAMPAGNE      = "Campagne";
    private static final String STATUT_SUCCESS       = "SUCCESS";
    private static final String CAMPAGNE_INTROUVABLE = "Campagne introuvable";

    private final CampagneRepository campagneRepository;
    private final RegionRepository regionRepository;
    private final ExcelCampagneParser excelCampagneParser;
    private final UtilisateurRepository utilisateurRepository;
    private final StructureRepository structureRepository;
    private final AuditLogService auditLogService;

    // ====================
    // CREATE
    // ====================
    public Campagne creerCampagne(CampagneRequestDTO dto, String emailCreateur, String ip) {
        // ✅ S112 — NoSuchElementException au lieu de RuntimeException
        Utilisateur createur = utilisateurRepository.findByEmail(emailCreateur)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur introuvable"));

        List<Region> regions = regionRepository.findAllById(dto.getRegionIds());
        if (regions.isEmpty()) {
            throw new IllegalArgumentException("Aucune région trouvée");
        }

        Campagne campagne = new Campagne();
        campagne.setLibelle(dto.getLibelle());
        campagne.setDateDebut(dto.getDateDebut());
        campagne.setDateFin(dto.getDateFin());
        campagne.setBudget(dto.getBudget());
        campagne.setDescription(dto.getDescription());
        campagne.setCode(dto.getCode());
        campagne.setRegions(regions);
        campagne.setStatut(StatutCampagne.BROUILLON);
        campagne.setCreateur(createur);

        Campagne saved = campagneRepository.save(campagne);

        auditLogService.log(AuditLogService.AuditLogRequest.builder()
                .email(emailCreateur)
                .action("CREATE")
                .entite(ENTITE_CAMPAGNE)
                .entiteId(saved.getId())
                .avant(null)
                .apres(saved)
                .ip(ip)
                .statut(STATUT_SUCCESS)
                .build());

        return saved;
    }

    // ====================
    // CREATE AVEC EXCEL
    // ====================
    public Campagne creerCampagneAvecExcel(CampagneRequestDTO dto, MultipartFile fichierExcel,
                                           String emailCreateur, String ip) {
        // ✅ S112
        Utilisateur createur = utilisateurRepository.findByEmail(emailCreateur)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur introuvable : " + emailCreateur));

        List<Region> regions = excelCampagneParser.extraireRegions(fichierExcel);
        if (regions.isEmpty()) {
            throw new IllegalArgumentException("Aucune région valide");
        }

        Campagne campagne = new Campagne();
        campagne.setLibelle(dto.getLibelle());
        campagne.setDateDebut(dto.getDateDebut());
        campagne.setDateFin(dto.getDateFin());
        campagne.setBudget(dto.getBudget());
        campagne.setDescription(dto.getDescription());
        campagne.setCode(dto.getCode());
        campagne.setRegions(regions);
        campagne.setStatut(StatutCampagne.BROUILLON);
        campagne.setCreateur(createur);

        Campagne campagneSauvee = campagneRepository.save(campagne);

        List<Structure> structures = excelCampagneParser.extraireStructures(fichierExcel);
        structures.forEach(s -> s.setCampagne(campagneSauvee));
        structureRepository.saveAll(structures);

        // ✅ S106 — logger.info au lieu de System.out.println
        log.info("=== {} structures sauvées pour campagne ID: {}", structures.size(), campagneSauvee.getId());

        auditLogService.log(AuditLogService.AuditLogRequest.builder()
                .email(emailCreateur)
                .action("CREATE_EXCEL")
                .entite(ENTITE_CAMPAGNE)
                .entiteId(campagneSauvee.getId())
                .avant(null)
                .apres(campagneSauvee)
                .ip(ip)
                .statut(STATUT_SUCCESS)
                .build());

        return campagneSauvee;
    }

    // ====================
    // READ
    // ====================
    public List<Campagne> getToutesCampagnes() {
        return campagneRepository.findAll();
    }

    public Campagne getCampagneParId(Long id) {
        // ✅ S112 + S1192
        return campagneRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(CAMPAGNE_INTROUVABLE));
    }

    public List<Campagne> getCampagnesActives() {
        return campagneRepository.findByStatut(StatutCampagne.ACTIVE);
    }

    // ====================
    // UPDATE
    // ====================
    public Campagne mettreAJourCampagne(Long id, CampagneRequestDTO dto, String email, String ip) {
        // ✅ S1192
        Campagne avant = campagneRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(CAMPAGNE_INTROUVABLE));

        String snapshotAvant = avant.getLibelle() + " | " + avant.getStatut();

        if (dto.getLibelle() != null)     avant.setLibelle(dto.getLibelle());
        if (dto.getDateDebut() != null)   avant.setDateDebut(dto.getDateDebut());
        if (dto.getDateFin() != null)     avant.setDateFin(dto.getDateFin());
        if (dto.getBudget() != null)      avant.setBudget(dto.getBudget());
        if (dto.getDescription() != null) avant.setDescription(dto.getDescription());
        if (dto.getCode() != null)        avant.setCode(dto.getCode());

        if (dto.getStatut() != null) {
            try {
                avant.setStatut(StatutCampagne.valueOf(dto.getStatut().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // IllegalArgumentException déjà spécifique — pas de RuntimeException ici
                throw new IllegalArgumentException("Statut invalide : " + dto.getStatut());
            }
        }

        if (dto.getRegionIds() != null && !dto.getRegionIds().isEmpty()) {
            List<Region> regions = regionRepository.findAllById(dto.getRegionIds());
            avant.setRegions(regions);
        }

        Campagne apres = campagneRepository.save(avant);

        auditLogService.log(AuditLogService.AuditLogRequest.builder()
                .email(email)
                .action("UPDATE")
                .entite(ENTITE_CAMPAGNE)
                .entiteId(id)
                .avant(snapshotAvant)
                .apres(apres)
                .ip(ip)
                .statut(STATUT_SUCCESS)
                .build());

        return apres;
    }

    // ====================
    // DELETE
    // ====================
    public void supprimerCampagne(Long id, String email, String ip) {
        // ✅ S1192
        Campagne campagne = campagneRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(CAMPAGNE_INTROUVABLE));

        auditLogService.log(AuditLogService.AuditLogRequest.builder()
                .email(email)
                .action("DELETE")
                .entite(ENTITE_CAMPAGNE)
                .entiteId(id)
                .avant(campagne)
                .apres(null)
                .ip(ip)
                .statut(STATUT_SUCCESS)
                .build());

        campagneRepository.delete(campagne);
    }


    // ====================
    //  Securiser l'affichage de la page campagne par code
    // ====================


public CampagnePubliqueDTO getCampagneParCode(String code) {
        Campagne campagne = campagneRepository
                .findByCodeAndStatut(code, StatutCampagne.ACTIVE)
                .orElseThrow(CampagneInvalideException::new);

        LocalDate today = LocalDate.now();
        if (today.isBefore(campagne.getDateDebut()) || today.isAfter(campagne.getDateFin())) {
            throw new CampagneInvalideException();
        }

        return new CampagnePubliqueDTO(
                campagne.getId(),
                campagne.getLibelle(),
                campagne.getDateDebut(),
                campagne.getDateFin(),
                campagne.getStatut().name()
        );
    }



    // ====================
    // LOGIQUE METIER
    // ====================
    public Campagne activerCampagne(Long id, String email, String ip) {
        Campagne campagne = getCampagneParId(id);
        // ✅ S112 — IllegalStateException au lieu de RuntimeException
        if (campagne.getStatut() != StatutCampagne.BROUILLON) {
            throw new IllegalStateException("Seulement les campagnes en brouillon peuvent être activées");
        }
        campagne.setStatut(StatutCampagne.ACTIVE);
        Campagne saved = campagneRepository.save(campagne);

        auditLogService.log(AuditLogService.AuditLogRequest.builder()
                .email(email)
                .action("ACTIVER")
                .entite(ENTITE_CAMPAGNE)
                .entiteId(id)
                .avant(StatutCampagne.BROUILLON)
                .apres(StatutCampagne.ACTIVE)
                .ip(ip)
                .statut(STATUT_SUCCESS)
                .build());

        return saved;
    }

    public Campagne cloturerCampagne(Long id, String email, String ip) {
        Campagne campagne = getCampagneParId(id);
        // ✅ S112 — IllegalStateException au lieu de RuntimeException
        if (campagne.getStatut() != StatutCampagne.ACTIVE) {
            throw new IllegalStateException("Seules les campagnes actives peuvent être clôturées");
        }
        campagne.setStatut(StatutCampagne.CLOTUREE);
        Campagne saved = campagneRepository.save(campagne);

        auditLogService.log(AuditLogService.AuditLogRequest.builder()
                .email(email)
                .action("CLOTURER")
                .entite(ENTITE_CAMPAGNE)
                .entiteId(id)
                .avant(StatutCampagne.ACTIVE)
                .apres(StatutCampagne.CLOTUREE)
                .ip(ip)
                .statut(STATUT_SUCCESS)
                .build());

        return saved;
    }

    public List<Campagne> getCampagnesParCreateur(String email) {
        return campagneRepository.findByCreateurEmail(email);
    }
}
