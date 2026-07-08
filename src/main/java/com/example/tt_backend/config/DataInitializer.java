package com.example.tt_backend.config;

import com.example.tt_backend.entity.RoleType;
import com.example.tt_backend.entity.Utilisateur;
import com.example.tt_backend.repository.UtilisateurRepository;
import com.example.tt_backend.service.PasswordPolicyService;
import com.example.tt_backend.util.EmailServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UtilisateurRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailServiceImpl emailService;
    private final PasswordPolicyService passwordPolicyService; // ← ajouter

    private static final Integer SUPERADMIN_MATRICULE = 74151;
    private static final String  SUPERADMIN_EMAIL     = "azizchahlaoui7@gmail.com";
    private static final String  SUPERADMIN_NOM       = "chahlaoui";
    private static final String  SUPERADMIN_PRENOM    = "aziz";

    @Override
    public void run(String... args) {
        boolean exists = userRepository.findByMatricule(SUPERADMIN_MATRICULE).isPresent();

        if (!exists) {
            // ── Mot de passe fort généré ────────────────────────────────
            String tempPassword = passwordPolicyService.generateTempPassword();

            Utilisateur superAdmin = new Utilisateur();
            superAdmin.setNom(SUPERADMIN_NOM);
            superAdmin.setPrenom(SUPERADMIN_PRENOM);
            superAdmin.setEmail(SUPERADMIN_EMAIL);
            superAdmin.setMatricule(SUPERADMIN_MATRICULE);
            superAdmin.setPassword(passwordEncoder.encode(tempPassword)); // ← plus le matricule
            superAdmin.setRole(RoleType.SUPERADMIN);
            superAdmin.setEnabled(true);
            superAdmin.setMustChangePassword(true); // ← forcer changement

            userRepository.save(superAdmin);
            log.info("✅ SuperAdmin créé — Matricule: {}", SUPERADMIN_MATRICULE);

            try {
                emailService.sendSuperAdminWelcomeEmail(
                        SUPERADMIN_EMAIL,
                        SUPERADMIN_NOM,
                        SUPERADMIN_PRENOM,
                        SUPERADMIN_MATRICULE,
                        tempPassword  // ← passer le vrai mot de passe
                );
                log.info("📧 Email SuperAdmin envoyé à {}", SUPERADMIN_EMAIL);
            } catch (Exception e) {
                log.error("❌ Échec email SuperAdmin : {}", e.getMessage());
            }

        } else {
            log.info("ℹ️ SuperAdmin déjà existant, aucune action nécessaire.");
        }
    }
}
