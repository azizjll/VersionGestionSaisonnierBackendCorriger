package com.example.tt_backend.service;

import com.example.tt_backend.entity.Region;
import com.example.tt_backend.entity.Structure;
import com.example.tt_backend.entity.StructureType;
import com.example.tt_backend.repository.RegionRepository;
import com.example.tt_backend.repository.StructureRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
public class ExcelReaderService {

    private final StructureRepository structureRepository;
    private final RegionRepository regionRepository;

    public ExcelReaderService(StructureRepository structureRepository,
                              RegionRepository regionRepository) {
        this.structureRepository = structureRepository;
        this.regionRepository = regionRepository;
    }

    public void importStructures(InputStream is) {

        Set<String> nomsExcel = new HashSet<>();

        try (Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) {
                    continue;
                }

                processRow(row, nomsExcel);
            }

            log.info("Import terminé — {} structures traitées", nomsExcel.size());

        } catch (Exception e) {
            log.error("Erreur import Excel", e);
            throw new RuntimeException("Erreur import Excel", e);
        }
    }

    // ===================== LOGIQUE LIGNE =====================
    private void processRow(Row row, Set<String> nomsExcel) {

        String regionNom = getCellString(row, 0);
        String structureNom = getCellString(row, 1);

        if (regionNom.isBlank() || structureNom.isBlank()) {
            return;
        }

        Region region = regionRepository.findByNom(regionNom).orElse(null);

        if (region == null) {
            log.warn("Région non trouvée : {}", regionNom);
            return;
        }

        nomsExcel.add(structureNom);

        String type = getCellString(row, 2);
        String adresse = getCellString(row, 3);
        int autorisesTotal = getCellInt(row, 4);
        // ✅ colonne "recrutes" (col 5) ignorée à l'import : on ne réinjecte pas
        //    de recrutés existants depuis Excel, ça viendrait fausser les compteurs.

        Structure structure = structureRepository
                .findByNomAndRegion(structureNom, region)
                .orElse(new Structure());

        int autorisesJuillet = autorisesTotal / 2;
        int autorisesAout    = autorisesTotal - autorisesJuillet;

        structure.setNom(structureNom);
        structure.setAdresse(adresse);
        structure.setRegion(region);
        structure.setAutorisesJuillet(autorisesJuillet);
        structure.setAutorisesAout(autorisesAout);

        // ⚠️ Ne pas écraser recrutesJuillet/recrutesAout si la structure existe déjà,
        // sinon tu perds le suivi des candidatures déjà affectées.
        if (structure.getId() == null) {
            structure.setRecrutesJuillet(0);
            structure.setRecrutesAout(0);
        }

        applyType(structure, type, structureNom);

        structureRepository.save(structure);

        log.info("Structure sauvegardée : {}", structureNom);
    }

    // ===================== TYPE SAFE =====================
    private void applyType(Structure structure, String type, String structureNom) {
        try {
            structure.setType(StructureType.valueOf(type.trim().toUpperCase()));
        } catch (Exception e) {
            log.warn("Type invalide '{}' pour structure {}", type, structureNom);
            structure.setType(StructureType.ESPACE_COMMERCIAL);
        }
    }

    // ===================== HELPERS =====================
    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            default -> "";
        };
    }

    private int getCellInt(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return 0;

        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Integer.parseInt(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    yield 0;
                }
            }
            default -> 0;
        };
    }
}