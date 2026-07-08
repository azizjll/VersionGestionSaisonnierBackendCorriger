package com.example.tt_backend.repository;

import com.example.tt_backend.entity.PasswordResetToken;
import com.example.tt_backend.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByUser(Utilisateur user);

}
