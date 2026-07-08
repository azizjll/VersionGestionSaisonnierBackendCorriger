package com.example.tt_backend.service;

import com.example.tt_backend.dto.SaisonnierDTO;
import com.example.tt_backend.dto.UpdatePaieRequest;
import com.example.tt_backend.entity.Saisonnier;
import com.example.tt_backend.repository.AffectationRepository;
import com.example.tt_backend.repository.SaisonnierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SaisonnierService {

    private final SaisonnierRepository repo;
    private final AffectationRepository affectationRepo;
    private final AuditLogService auditLogService;

    // ─────────────────────────────
    // CONSTANTES (fix S1192)
    // ─────────────────────────────
    private static final String ENTITE = "Saisonnier";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String ACTION_READ_ALL = "READ_ALL";
    private static final String ACTION_READ = "READ";
    private static final String ACTION_READ_BY_REGION = "READ_BY_CAMPAGNE_REGION";
    private static final String ACTION_READ_BY_STRUCTURE = "READ_BY_CAMPAGNE_STRUCTURE";
    private static final String ACTION_UPDATE_ABSENCES = "UPDATE_ABSENCES";

    // ====================
    // READ ALL
    // ====================
    public List<SaisonnierDTO> findAll(String email, String ip) {

        List<SaisonnierDTO> result = repo.findAll()
                .stream()
                .map(s -> SaisonnierDTO.from(s, null))
                .toList(); // FIX S6204

        auditLogService.log(AuditLogService.AuditLogRequest.builder()
                .email(email)
                .action(ACTION_READ_ALL)
                .entite(ENTITE)
                .entiteId(null)
                .avant(null)
                .apres(result.size() + " résultats")
                .ip(ip)
                .statut(STATUS_SUCCESS)
                .build());

        return result;
    }

    // ====================
    // READ BY ID
    // ====================
    public SaisonnierDTO findById(Long id, String email, String ip) {

        SaisonnierDTO result = repo.findById(id)
                .map(s -> SaisonnierDTO.from(s, null))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Saisonnier " + id + " introuvable"
                ));

        auditLogService.log(AuditLogService.AuditLogRequest.builder()
                .email(email)
                .action(ACTION_READ)
                .entite(ENTITE)
                .entiteId(id)
                .avant(null)
                .apres(result)
                .ip(ip)
                .statut(STATUS_SUCCESS)
                .build());

        return result;
    }

    // ====================
    // BY CAMPAGNE + REGION
    // ====================
    public List<SaisonnierDTO> findByCampagneAndRegion(Long campagneId, Long regionId, String email, String ip) {

        List<SaisonnierDTO> result = repo.findAll()
                .stream()
                .filter(s -> s.getRegion() != null && s.getRegion().getId().equals(regionId))
                .flatMap(s -> s.getCandidatures().stream()
                        .filter(c -> c.getCampagne().getId().equals(campagneId)
                                && "ACCEPTEE".equals(c.getStatut().name()))
                        .map(c -> SaisonnierDTO.from(s, c.getStatut().name())))
                .toList(); // FIX S6204

        auditLogService.log(AuditLogService.AuditLogRequest.builder()
                .email(email)
                .action(ACTION_READ_BY_REGION)
                .entite(ENTITE)
                .entiteId(campagneId)
                .avant(null)
                .apres(result.size() + " résultats — regionId: " + regionId)
                .ip(ip)
                .statut(STATUS_SUCCESS)
                .build());

        return result;
    }

    // ====================
    // BY CAMPAGNE + STRUCTURE
    // ====================
    public List<SaisonnierDTO> findByCampagneAndStructure(Long campagneId, Long structureId, String email, String ip) {

        List<Long> saisonnierIds = affectationRepo.findAll()
                .stream()
                .filter(a -> a.getCampagne().getId().equals(campagneId)
                        && a.getStructure().getId().equals(structureId))
                .map(a -> a.getSaisonnier().getId())
                .toList(); // FIX S6204

        List<SaisonnierDTO> result = repo.findAllById(saisonnierIds)
                .stream()
                .flatMap(s -> s.getCandidatures().stream()
                        .filter(c -> c.getCampagne().getId().equals(campagneId)
                                && "ACCEPTEE".equals(c.getStatut().name()))
                        .map(c -> SaisonnierDTO.from(s, c.getStatut().name())))
                .toList(); // FIX S6204

        auditLogService.log(AuditLogService.AuditLogRequest.builder()
                .email(email)
                .action(ACTION_READ_BY_STRUCTURE)
                .entite(ENTITE)
                .entiteId(campagneId)
                .avant(null)
                .apres(result.size() + " résultats — structureId: " + structureId)
                .ip(ip)
                .statut(STATUS_SUCCESS)
                .build());

        return result;
    }

    // ====================
    // UPDATE
    // ====================
    public SaisonnierDTO updateAbsences(Long id, UpdatePaieRequest req, String email, String ip) {

        Saisonnier s = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Saisonnier " + id + " introuvable"
                ));

        String snapshotAvant = "absences: " + s.getAbsences();

        if (req.getAbsences() != null) {
            s.setAbsences(req.getAbsences());
        }

        repo.save(s);

        SaisonnierDTO result = SaisonnierDTO.from(s, null);

        auditLogService.log(AuditLogService.AuditLogRequest.builder()
                .email(email)
                .action(ACTION_UPDATE_ABSENCES)
                .entite(ENTITE)
                .entiteId(id)
                .avant(snapshotAvant)
                .apres("absences: " + s.getAbsences())
                .ip(ip)
                .statut(STATUS_SUCCESS)
                .build());

        return result;
    }
}