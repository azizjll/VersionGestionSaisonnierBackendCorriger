package com.example.tt_backend.repository;

import com.example.tt_backend.entity.Region;
import com.example.tt_backend.entity.StatutCampagne;
import com.example.tt_backend.entity.Structure;
import com.example.tt_backend.entity.StructureType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StructureRepository extends JpaRepository<Structure, Long> {



    List<Structure> findByRegionId(Long regionId);




    List<Structure> findAll();
    Optional<Structure> findByNomAndRegion(String nom, Region region);

    @Query("SELECT s FROM Structure s WHERE s.region IN " +
            "(SELECT r FROM Campagne c JOIN c.regions r WHERE c.statut = 'ACTIVE')")
    List<Structure> findStructuresDeCampagneActive();

    @Query("SELECT DISTINCT s FROM Structure s WHERE s.region IN " +
            "(SELECT r FROM Campagne c JOIN c.regions r WHERE c.id = :campagneId)")
    List<Structure> findStructuresByCampagneId(@Param("campagneId") Long campagneId);

    // ← NOUVEAU : filtrer directement par campagne
    List<Structure> findByCampagneId(Long campagneId);



    List<Structure> findByRegionIdAndCampagneId(Long regionId, Long campagneId);
    List<Structure> findByCampagneIdAndRegionId(Long campagneId, Long regionId);
    Optional<Structure> findByNomIgnoreCaseAndRegion(String nom, Region region);

    @Query("SELECT s FROM Structure s WHERE LOWER(s.nom) = LOWER(:nom) " +
            "AND s.region = :region AND s.campagne.id = :campagneId")
    Optional<Structure> findByNomIgnoreCaseAndRegionAndCampagneId(
            @Param("nom") String nom,
            @Param("region") Region region,
            @Param("campagneId") Long campagneId
    );

}