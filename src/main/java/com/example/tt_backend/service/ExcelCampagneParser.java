package com.example.tt_backend.service;

import com.example.tt_backend.entity.Region;
import com.example.tt_backend.entity.Structure;
import com.example.tt_backend.entity.StructureType;
import com.example.tt_backend.repository.RegionRepository;
import com.example.tt_backend.repository.StructureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelCampagneParser {

    private final RegionRepository regionRepository;

    // ─────────────────────────────────────────────
    // REGIONS
    // ─────────────────────────────────────────────
    public List<Region> extraireRegions(MultipartFile file) {

        Map<String, Region> regionsMap = new LinkedHashMap<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {

                if (row.getRowNum() == 0) {
                    continue;
                }

                String regionNom = getCellString(row, 0);

                if (regionNom.isBlank()) {
                    continue;
                }

                Region region = regionsMap.computeIfAbsent(regionNom, key ->
                        regionRepository.findByNom(key)
                                .orElseGet(() -> regionRepository.save(new Region(key)))
                );

                regionsMap.put(regionNom, region);
            }

        } catch (Exception e) {
            throw new IllegalStateException("Erreur lecture Excel (regions)", e);
        }

        return new ArrayList<>(regionsMap.values());
    }

    // ─────────────────────────────────────────────
    // STRUCTURES
    // ─────────────────────────────────────────────
    public List<Structure> extraireStructures(MultipartFile file) {

        Map<String, Region> regionsMap = new LinkedHashMap<>();
        List<Structure> structures = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {

                if (row.getRowNum() == 0) {
                    continue;
                }

                String regionNom = getCellString(row, 0);

                if (regionNom.isBlank()) {
                    continue;
                }

                Region region = regionsMap.computeIfAbsent(regionNom, key ->
                        regionRepository.findByNom(key)
                                .orElseGet(() -> regionRepository.save(new Region(key)))
                );

                String nomStructure = getCellString(row, 1);
                String typeRaw = getCellString(row, 2);
                int autorises = getCellInt(row, 3);

                if (nomStructure.isBlank()) {
                    continue;
                }

                Structure s = new Structure();
                s.setNom(nomStructure);
                s.setAutorises(autorises);
                s.setRecrutes(0);
                s.setRegion(region);
                s.setType(parseType(typeRaw));

                structures.add(s);
            }

        } catch (Exception e) {
            throw new IllegalStateException("Erreur lecture Excel (structures)", e);
        }

        return structures;
    }

    // ─────────────────────────────────────────────
    // SAFE TYPE PARSING (évite try dans boucle)
    // ─────────────────────────────────────────────
    private StructureType parseType(String typeRaw) {
        try {
            return StructureType.valueOf(typeRaw.toUpperCase());
        } catch (Exception e) {
            return StructureType.ESPACE_COMMERCIAL;
        }
    }

    // ─────────────────────────────────────────────
    // CELL STRING
    // ─────────────────────────────────────────────
    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            default -> "";
        };
    }

    // ─────────────────────────────────────────────
    // CELL INT (LOGS SUPPRIMÉS → S106 FIX)
    // ─────────────────────────────────────────────
    private int getCellInt(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            log.warn("Cellule null row={} col={}", row.getRowNum(), col);
            return 0;
        }

        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Integer.parseInt(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    yield 0;
                }
            }
            case FORMULA -> (int) cell.getNumericCellValue();
            default -> 0;
        };
    }
}