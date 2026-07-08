package com.example.tt_backend.repository;

import com.example.tt_backend.entity.VerificationToken;
import com.example.tt_backend.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);
    Optional<VerificationToken> findByUser(Utilisateur user);
}
