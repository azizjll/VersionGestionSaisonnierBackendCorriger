package com.example.tt_backend.repository;

import com.example.tt_backend.entity.Region;
import com.example.tt_backend.entity.Saisonnier;
import com.example.tt_backend.entity.StatutCandidature;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SaisonnierRepository extends JpaRepository<Saisonnier, Long> {



    @Query("SELECT s FROM Saisonnier s JOIN s.candidatures c WHERE c.statut = :statut AND s.region = :region")
    List<Saisonnier> findSaisonniersAcceptesParRegion(
            @Param("region") Region region,
            @Param("statut") StatutCandidature statut
    );

    Optional<Saisonnier> findByCin(String  cin);


}
