package com.example.tt_backend.service;

import com.example.tt_backend.dto.*;
import com.example.tt_backend.entity.*;
import com.example.tt_backend.repository.*;
import com.example.tt_backend.util.EmailServiceImpl;
import com.example.tt_backend.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.UUID;

// ✅ S106 — @Slf4j fournit le logger
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilisateurRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final EmailServiceImpl emailService;
    private final RegionRepository regionRepository;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();


    // ✅ S2140 — Random instance pour nextInt()
    private final Random random = new Random();

    // -------------------- SIGNUP --------------------
    public void signup(SignupRequest request) {
        Utilisateur user = new Utilisateur();
        user.setNom(request.getNom());
        user.setPrenom(request.getPrenom());
        user.setEmail(request.getEmail());
        user.setCin(request.getCin());
        user.setTelephone(request.getTelephone());
        user.setRole(request.getRole());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(false);

        if (request.getRegionId() != null) {
            // ✅ S112 — NoSuchElementException au lieu de RuntimeException
            Region region = regionRepository.findById(request.getRegionId())
                    .orElseThrow(() -> new NoSuchElementException("Region non trouvée"));
            user.setRegion(region);
        }

        userRepository.save(user);

        VerificationToken token = new VerificationToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().plusDays(1));
        verificationTokenRepository.save(token);

        emailService.sendVerificationEmail(user.getEmail(), token.getToken());
    }

    // -------------------- SIGNIN ADMINISTRATEUR --------------------
    public String signinAdministrateur(SigninAdministrateurRequest request) {

        // ✅ S106 — logger.debug au lieu de System.out.println
        log.debug("=== SIGNIN DEBUG ===");
        log.debug("Matricule reçu : {}", request.getMatricule());

        List<Utilisateur> users = userRepository.findAllByMatricule(request.getMatricule());
        log.debug("Nombre d'utilisateurs trouvés : {}", users.size());

        if (users.isEmpty()) {
            // ✅ S112
            throw new NoSuchElementException("Matricule introuvable");
        }

        Utilisateur user;

        if (users.size() > 1) {
            log.debug("Plusieurs users, filtrage sur campagne ACTIVE...");
            for (Utilisateur u : users) {
                log.debug("  - user id={} campagne={}", u.getId(),
                        u.getCampagne() != null ? u.getCampagne().getStatut() : "NULL");
            }
            // ✅ S112
            user = users.stream()
                    .filter(u -> u.getCampagne() != null
                            && u.getCampagne().getStatut() == StatutCampagne.ACTIVE)
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException("Aucune campagne active pour cet utilisateur."));
        } else {
            user = users.get(0);
        }

        log.debug("User sélectionné : {} | enabled={}", user.getEmail(), user.getEnabled());

        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), request.getPassword())
        );
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        return jwtUtils.generateToken(userDetails);
    }

    // -------------------- VERIFY EMAIL --------------------
    public void verifyToken(String tokenStr) {
        VerificationToken token = verificationTokenRepository.findByToken(tokenStr)
                // ✅ S112
                .orElseThrow(() -> new NoSuchElementException("Token invalide"));
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Token expiré");
        }
        Utilisateur user = token.getUser();
        user.setEnabled(true);
        userRepository.save(user);
        verificationTokenRepository.delete(token);
    }

    // -------------------- SIGNIN --------------------
    public String signin(SigninRequest request) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        return jwtUtils.generateToken(userDetails);
    }

    // -------------------- FORGOT PASSWORD --------------------
    @Transactional
    public void forgotPassword(String email) {
        Utilisateur user = userRepository.findByEmail(email)
                // ✅ S112
                .orElseThrow(() -> new NoSuchElementException("Utilisateur non trouvé"));

        PasswordResetToken token = passwordResetTokenRepository
                .findByUser(user)
                .orElse(new PasswordResetToken());

        token.setUser(user);
        // ✅ S2140 — random.nextInt() au lieu de Math.random()
        token.setToken(generateOtp());
        token.setExpiryDate(LocalDateTime.now().plusMinutes(15));

        passwordResetTokenRepository.save(token);
        emailService.sendPasswordResetEmail(user.getEmail(), token.getToken());
    }

    /**
     * Génère un OTP à 6 chiffres cryptographiquement sûr.
     * SecureRandom est conforme NIST SP 800-90A — safe pour usage sécurité.
     */
    private String generateOtp() {
        int otp = SECURE_RANDOM.nextInt(1_000_000); // [0, 999999]
        return String.format("%06d", otp);
    }

    // -------------------- RESET PASSWORD --------------------
    public void resetPassword(NewPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByToken(request.getToken())
                // ✅ S112
                .orElseThrow(() -> new NoSuchElementException("Code invalide ou déjà utilisé"));

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            passwordResetTokenRepository.delete(token);
            throw new IllegalStateException("Code expiré, veuillez en demander un nouveau");
        }

        Utilisateur user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        passwordResetTokenRepository.delete(token);
    }

    // -------------------- LOAD USER --------------------
    public UserDetails loadUserByUsername(String email) {
        List<Utilisateur> users = userRepository.findAllByEmail(email);

        if (users.isEmpty()) {
            // ✅ S112
            throw new NoSuchElementException("Utilisateur non trouvé");
        }

        Utilisateur utilisateur = users.size() > 1
                ? users.stream()
                .filter(u -> u.getCampagne() != null
                        && u.getCampagne().getStatut() == StatutCampagne.ACTIVE)
                .findFirst()
                .orElse(users.get(0))
                : users.get(0);

        return User.builder()
                .username(utilisateur.getEmail())
                .password(utilisateur.getPassword())
                .disabled(!utilisateur.getEnabled())
                .authorities(utilisateur.getRole().name())
                .build();
    }

    // -------------------- FIND BY EMAIL --------------------
    public Utilisateur findByEmail(String email) {
        return userRepository.findByEmail(email)
                // ✅ S112
                .orElseThrow(() -> new NoSuchElementException("Utilisateur non trouvé"));
    }
}