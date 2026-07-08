package com.example.tt_backend.service;

import com.example.tt_backend.entity.*;
import com.example.tt_backend.repository.CampagneRepository;
import com.example.tt_backend.repository.RegionRepository;
import com.example.tt_backend.repository.StructureRepository;
import com.example.tt_backend.repository.UtilisateurRepository;
import com.example.tt_backend.util.EmailServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.SecureRandom;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelImportService {

    private final UtilisateurRepository userRepository;
    private final RegionRepository regionRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailServiceImpl emailService;
    private final CampagneRepository campagneRepository;
    private final StructureRepository structureRepository;



    public ImportResult importFromExcel(MultipartFile file) {
        int created = 0;
        int skipped = 0;
        int deleted = 0;
        List<String> errors = new ArrayList<>();

        // Rôles gérés par cet import
        Set<RoleType> rolesGeres = Set.of(
                RoleType.SUPERADMIN,
                RoleType.ADMIN,
                RoleType.RH_REGIONAL
        );

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            // ── PASSE 1 : parser toutes les lignes valides du fichier ──────────
            List<Map<String, String>> lignesValides = new ArrayList<>();
            Set<RoleType> rolesTrouvesDansFichier = new HashSet<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                Integer matricule = readInt(row.getCell(0));
                String nomPrenom  = readString(row.getCell(1));
                String email      = readString(row.getCell(2));
                String roleExcel  = readString(row.getCell(3));
                String direction  = readString(row.getCell(4));

                if (matricule == null || email == null || email.isBlank()) {
                    errors.add("Ligne " + (i + 1) + " ignorée: matricule ou email manquant");
                    skipped++;
                    continue;
                }

                RoleType role = mapRole(roleExcel);
                if (role == null) {
                    errors.add("Ligne " + (i + 1) + " — rôle inconnu: " + roleExcel);
                    skipped++;
                    continue;
                }

                // On n'accepte que les rôles gérés par cet import
                if (!rolesGeres.contains(role)) {
                    errors.add("Ligne " + (i + 1) + " — rôle non autorisé dans cet import: " + roleExcel);
                    skipped++;
                    continue;
                }

                Map<String, String> ligne = new HashMap<>();
                ligne.put("matricule", String.valueOf(matricule));
                ligne.put("nomPrenom", nomPrenom != null ? nomPrenom : "");
                ligne.put("email", email.trim().toLowerCase());
                ligne.put("role", roleExcel != null ? roleExcel.trim() : "");
                ligne.put("direction", direction != null ? direction : "");
                lignesValides.add(ligne);
                rolesTrouvesDansFichier.add(role);
            }

            // ── SUPPRESSION : supprimer tous les utilisateurs des rôles gérés ──
            // Uniquement les rôles effectivement présents dans le nouveau fichier
            List<Utilisateur> toDelete = userRepository.findAll().stream()
                    .filter(u -> rolesTrouvesDansFichier.contains(u.getRole()))
                    .toList();

            for (Utilisateur u : toDelete) {

                List<Campagne> campagnes =
                        campagneRepository.findByCreateur(u);

                for (Campagne c : campagnes) {
                    c.setCreateur(null);
                }

                campagneRepository.saveAll(campagnes);

                userRepository.delete(u);

                deleted++;
                log.info("Compte supprimé (écrasement) — matricule: {}, rôle: {}", u.getMatricule(), u.getRole());
            }

            // ── PASSE 2 : insérer tous les utilisateurs du nouveau fichier ─────
            for (Map<String, String> ligne : lignesValides) {
                try {
                    Integer matricule = Integer.parseInt(ligne.get("matricule"));
                    String email      = ligne.get("email");
                    String roleExcel  = ligne.get("role");
                    String nomPrenom  = ligne.get("nomPrenom");
                    String direction  = ligne.get("direction");

                    RoleType role = mapRole(roleExcel);

                    String[] parts = splitNomPrenom(nomPrenom);
                    String nom    = parts[0];
                    String prenom = parts[1];

                    Utilisateur user = new Utilisateur();
                    user.setNom(nom);
                    user.setPrenom(prenom);
                    user.setEmail(email);
                    user.setMatricule(matricule);
                    String tempPassword = generateTempPassword();
                    user.setPassword(passwordEncoder.encode(tempPassword));
                    user.setMustChangePassword(true);
                    user.setRole(role);
                    user.setEnabled(true);

                    if (role == RoleType.RH_REGIONAL && !direction.isBlank()) {
                        Optional<Region> regionOpt = regionRepository.findByNomIgnoreCase(direction.trim());
                        Region region = regionOpt.orElseGet(() -> {
                            Region r = new Region();
                            r.setNom(direction.trim());
                            log.info("Nouvelle région créée: {}", direction);
                            return regionRepository.save(r);
                        });
                        user.setRegion(region);
                    }

                    userRepository.save(user);
                    created++;
                    log.info("Compte créé — matricule: {}, rôle: {}", matricule, role);

                    try {
                        emailService.sendWelcomeRHEmail(email, nom, prenom, matricule, tempPassword);
                        log.info("📧 Email envoyé à {}", email);
                    } catch (Exception e) {
                        log.error("❌ Échec email pour {} : {}", email, e.getMessage());
                    }

                } catch (Exception e) {
                    errors.add("Erreur lors de la création: " + e.getMessage());
                    skipped++;
                    log.error("Erreur création utilisateur: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Impossible de lire le fichier Excel: " + e.getMessage(), e);
        }

        return new ImportResult(created, skipped, deleted, errors);
    }
    // ── DTO mis à jour ──────────────────────────────────────────────────
    public record ImportResult(int created, int skipped, int deleted, List<String> errors) {}

    // ---- Mapping rôles Excel → RoleType ----
    private RoleType mapRole(String roleExcel) {
        if (roleExcel == null) return null;
        return switch (roleExcel.trim()) {
            case "SuperAdmin"             -> RoleType.SUPERADMIN;
            case "Administrateur RH"      -> RoleType.ADMIN;
            case "Responsable RH"         -> RoleType.RH_REGIONAL;
            case "Responsable Structure"  -> RoleType.RESPONSABLE_STRUCTURE;
            default -> null;
        };
    }

    // ---- Décompose "ABIDI Bassem" en [nom="ABIDI", prenom="Bassem"] ----
    private String[] splitNomPrenom(String nomPrenom) {
        if (nomPrenom == null || nomPrenom.isBlank()) return new String[]{"", ""};
        String[] parts = nomPrenom.trim().split("\\s+", 2);
        if (parts.length == 1) return new String[]{parts[0], ""};
        return new String[]{parts[0], parts[1]};
    }

    // ---- Lecture cellule texte ----
    private String readString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default      -> null;
        };
    }

    // ---- Lecture cellule entier ----
    private Integer readInt(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING  -> {
                try { yield Integer.parseInt(cell.getStringCellValue().trim()); }
                catch (NumberFormatException e) { yield null; }
            }
            default -> null;
        };
    }

    // ---- Vérifier si une ligne est vide ----
    private boolean isRowEmpty(Row row) {
        for (int c = 0; c < 5; c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    // ---- DTO résultat de l'import ----





    public ImportResult importResponsablesStructure(MultipartFile file) {
        int created = 0;
        int skipped = 0;
        int deleted = 0;
        List<String> errors = new ArrayList<>();

        List<Campagne> campagnesActives = campagneRepository.findByStatut(StatutCampagne.ACTIVE);
        if (campagnesActives.isEmpty()) {
            throw new RuntimeException("Aucune campagne active. L'import est impossible.");
        }
        Campagne campagneActive = campagnesActives.get(0);

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            // ── SUPPRESSION : supprimer TOUS les RS de la campagne active ──────
            List<Utilisateur> toDelete = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == RoleType.RESPONSABLE_STRUCTURE)
                    .filter(u -> u.getCampagne() != null
                            && campagneActive.getId().equals(u.getCampagne().getId()))
                    .toList();

            for (Utilisateur u : toDelete) {
                userRepository.delete(u);
                deleted++;
                log.info("Compte RS supprimé — matricule: {}, email: {}", u.getMatricule(), u.getEmail());
            }

            // ✅ FORCER l'exécution des DELETE en base AVANT les INSERT
            userRepository.flush();

            // ── PASSE 2 : insérer tous les RS du nouveau fichier ───────────────
            Iterator<Row> rows = sheet.iterator();
            if (rows.hasNext()) rows.next(); // sauter l'en-tête

            while (rows.hasNext()) {
                Row row = rows.next();
                if (isRowEmpty(row)) continue;

                try {
                    Integer matricule    = readInt(row.getCell(0));
                    String  nomPrenom    = readString(row.getCell(1));
                    String  email        = readString(row.getCell(2));
                    String  roleExcel    = readString(row.getCell(3));
                    String  nomStructure = readString(row.getCell(4));

                    if (matricule == null || email == null || email.isBlank()) {
                        errors.add("Ligne " + (row.getRowNum() + 1) + " ignorée: matricule ou email manquant");
                        skipped++;
                        continue;
                    }

                    RoleType role = mapRole(roleExcel);
                    if (role != RoleType.RESPONSABLE_STRUCTURE) {
                        errors.add("Ligne " + (row.getRowNum() + 1) + " ignorée: rôle '" + roleExcel + "' non autorisé");
                        skipped++;
                        continue;
                    }

                    if (nomStructure == null || nomStructure.isBlank()) {
                        errors.add("Ligne " + (row.getRowNum() + 1) + " ignorée: structure manquante");
                        skipped++;
                        continue;
                    }

                    Structure structure = null;
                    Region regionTrouvee = null;

                    for (Region region : campagneActive.getRegions()) {
                        Optional<Structure> structOpt = structureRepository
                                .findByNomIgnoreCaseAndRegionAndCampagneId(
                                        nomStructure.trim(),
                                        region,
                                        campagneActive.getId()
                                );
                        if (structOpt.isPresent()) {
                            structure = structOpt.get();
                            regionTrouvee = region;
                            break;
                        }
                    }

                    if (structure == null) {
                        errors.add("Ligne " + (row.getRowNum() + 1) + " ignorée: structure '"
                                + nomStructure + "' introuvable dans la campagne active");
                        skipped++;
                        continue;
                    }

                    String[] parts = splitNomPrenom(nomPrenom);
                    String nom    = parts[0];
                    String prenom = parts[1];

                    String tempPassword = generateTempPassword();

                    Utilisateur user = new Utilisateur();
                    user.setNom(nom);
                    user.setPrenom(prenom);
                    user.setEmail(email.trim().toLowerCase());
                    user.setMatricule(matricule);
                    user.setPassword(passwordEncoder.encode(tempPassword));
                    user.setMustChangePassword(true);
                    user.setRole(RoleType.RESPONSABLE_STRUCTURE);
                    user.setEnabled(true);
                    user.setRegion(regionTrouvee);
                    user.setStructure(structure);
                    user.setCampagne(campagneActive);

                    userRepository.save(user);
                    created++;
                    log.info("Compte RS créé — matricule: {}, structure: {}, région: {}",
                            matricule, structure.getNom(), regionTrouvee.getNom());

                    try {
                        emailService.sendWelcomeRSEmail(
                                email.trim().toLowerCase(), nom, prenom, matricule, tempPassword
                        );
                        log.info("📧 Email envoyé à {}", email);
                    } catch (Exception e) {
                        log.error("❌ Échec email pour {} : {}", email, e.getMessage());
                    }

                } catch (Exception e) {
                    errors.add("Ligne " + (row.getRowNum() + 1) + " — erreur: " + e.getMessage());
                    skipped++;
                    log.error("Erreur ligne {}: {}", row.getRowNum() + 1, e.getMessage());
                }
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Impossible de lire le fichier Excel: " + e.getMessage(), e);
        }

        return new ImportResult(created, skipped, deleted, errors);
    }
    // ---- Génération d'un mot de passe temporaire fort ----
    private String generateTempPassword() {
        String upper   = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower   = "abcdefghijklmnopqrstuvwxyz";
        String digits  = "0123456789";
        String special = "@#$%&*!?";
        String all     = upper + lower + digits + special;

        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        // Garantir au moins un caractère de chaque catégorie obligatoire
        sb.append(upper.charAt(rng.nextInt(upper.length())));
        sb.append(digits.charAt(rng.nextInt(digits.length())));
        sb.append(special.charAt(rng.nextInt(special.length())));

        // Compléter jusqu'à 14 caractères
        for (int i = 3; i < 14; i++)
            sb.append(all.charAt(rng.nextInt(all.length())));

        // Mélanger pour éviter un pattern prévisible
        List<Character> chars = new ArrayList<>();
        for (char c : sb.toString().toCharArray()) chars.add(c);
        Collections.shuffle(chars, rng);

        StringBuilder result = new StringBuilder();
        chars.forEach(result::append);
        return result.toString();
    }
}