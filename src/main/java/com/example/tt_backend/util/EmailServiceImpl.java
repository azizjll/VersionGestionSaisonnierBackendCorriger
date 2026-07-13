package com.example.tt_backend.util;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl {

    private final JavaMailSender mailSender;

    // ✅ CONSTANTE (fix S1192)
    private static final String DEFAULT_FROM_EMAIL = "azizchahlaoui7@gmail.com";

    // =========================
    // BASE EMAIL METHOD
    // =========================
    private void sendEmail(String to, String subject, String message) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(DEFAULT_FROM_EMAIL);
        mailMessage.setTo(to);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);
        mailSender.send(mailMessage);
    }

    // =========================
    // AUTH EMAILS
    // =========================
    public void sendVerificationEmail(String to, String token) {
        String subject = "Activation de votre compte";
        String message = "Cliquez sur ce lien pour activer votre compte : "
                + "https://saisonnier.tunisietelecom.tn/auth/verify?token=" + token;


        sendEmail(to, subject, message);
    }

    public void sendPasswordResetEmail(String to, String token) {
        String subject = "Réinitialisation du mot de passe";
        String message = "Bonjour,\n\n"
                + "Voici votre code de réinitialisation :\n\n"
                + token + "\n\n"
                + "Ce code est valable 15 minutes.";

        sendEmail(to, subject, message);
    }

    // =========================
    // WELCOME EMAILS
    // =========================
    public void sendSuperAdminWelcomeEmail(String to, String nom, String prenom,
                                           Integer matricule, String tempPassword) {

        String contenu = """
        Bonjour %s %s,

        Votre compte SuperAdmin a été créé avec succès.

        Vous pouvez maintenant vous connecter à la plateforme via  le lien suivant : https://saisonnier.tunisietelecom.tn/home-ge

        ── Identifiants ──
        Matricule : %d
        Mot de passe : %s

        Cordialement
        Equipe DDRH
        """.formatted(prenom, nom, matricule, tempPassword);

        sendEmail(to, "🔐 Bienvenue SuperAdmin", contenu);
    }

    public void sendWelcomeRSEmail(String to, String nom, String prenom,
                                   Integer matricule, String tempPassword) {

        String contenu = """
        Bonjour %s %s,

        Votre compte Responsable Structure a été créé avec succès.

        Vous pouvez maintenant vous connecter à la plateforme via  le lien suivant : https://saisonnier.tunisietelecom.tn/home-ge

        Matricule : %d
        Mot de passe : %s

        Cordialement
        Equipe DDRH
        """.formatted(prenom, nom, matricule, tempPassword);

        sendEmail(to, "🏢 Bienvenue Responsable Structure", contenu);
    }

    public void sendWelcomeRHEmail(String to, String nom, String prenom,
                                   Integer matricule, String tempPassword) {

        String contenu = """
        Bonjour %s %s,

        Votre compte Responsable RH a été créé avec succès.

	Vous pouvez maintenant vous connecter à la plateforme via  le lien suivant : https://saisonnier.tunisietelecom.tn/home-ge

        Matricule : %d
        Mot de passe : %s

	Cordialement
 	Equipe DDRH
        """.formatted(prenom, nom, matricule, tempPassword);

        sendEmail(to, "🎉 Bienvenue RH", contenu);
    }

    // =========================
// CANDIDATURE EMAILS
// =========================
    public void sendCandidatureAccepteeEmail(
            String to,
            String prenomNom,
            String direction,
            String structureNom,
            String structureType,
            String moisTravail
    ) {
        String sujet = "✅ Candidature acceptée";

        String corps = """
    Bonjour %s,

    Nous avons le plaisir de vous informer que votre candidature pour le poste de saisonnier a été acceptée.

    ── Détails de votre affectation ──
    Direction : %s
    Structure : %s
    Type de structure : %s
    Période de travail : %s

    Nous vous contacterons prochainement pour les prochaines étapes.

    Cordialement
    Equipe DDRH
    """.formatted(
                prenomNom,
                direction != null ? direction : "-",
                structureNom != null ? structureNom : "-",
                formatTypeStructure(structureType),
                formatMoisTravail(moisTravail)
        );

        sendEmail(to, sujet, corps);
    }

    // ── Helpers privés pour formater l'affichage ──
    private String formatTypeStructure(String type) {
        if (type == null) return "-";
        return switch (type) {
            case "ESPACE_COMMERCIAL" -> "Espace Commercial";
            case "CENTRE_TECHNIQUE"  -> "Centre Technique";
            case "STRUCTURE_CENTRALE" -> "Structure Centrale";
            default -> type;
        };
    }

    private String formatMoisTravail(String mois) {
        if (mois == null) return "-";
        return switch (mois) {
            case "JUILLET" -> "Juillet";
            case "AOUT" -> "Août";
            case "JUILLET_AOUT" -> "Juillet et Août";
            default -> mois;
        };
    }

    public void sendCandidatureRefuseeEmail(String to, String prenomNom, String motifRefus) {
        String corps = """
    Bonjour %s,

    Nous vous informons que votre candidature pour le poste de saisonnier n'a malheureusement pas été retenue.

    Motif : %s

    Cordialement
    Equipe DDRH
    """.formatted(
                prenomNom,
                motifRefus != null && !motifRefus.isBlank() ? motifRefus : "-"
        );

        sendEmail(to, "❌ Candidature refusée", corps);
    }

    // =========================
    // DEMANDES RH
    // =========================
    public void envoyerDemandeAutorisationJuilletAout(
            String prenomSaisonnier,
            String nomSaisonnier,
            String cin,
            String directionRH,
            String commentaire,
            List<String> emailsAdmins
    ) {

        String sujet = "📋 Demande Juillet-Août - " + prenomSaisonnier + " " + nomSaisonnier;

        String corps = """
        Nom : %s %s
        CIN : %s
        Direction RH : %s
        Commentaire : %s
        """.formatted(
                prenomSaisonnier,
                nomSaisonnier,
                cin,
                directionRH,
                commentaire != null ? commentaire : "-"
        );

        for (String email : emailsAdmins) {
            sendEmail(email, sujet, corps);
        }
    }

    public void envoyerDemandeAutorisationQuotaParent(
            String prenomSaisonnier,
            String nomSaisonnier,
            String cin,
            String matriculeParent,
            String nomPrenomParent,
            int utilise,
            int autorises,
            String directionRH,
            String prenom,
            String nom,
            String commentaire,
            List<String> emailsAdmins
    ) {

        String sujet = "⚠️ Quota parent dépassé - " + prenomSaisonnier;

        String corps = """
        Saisonnier : %s %s
        CIN : %s

        Parent : %s (%s)
        Utilisation : %d/%d

        RH : %s %s
        Direction : %s
        Commentaire : %s
        """.formatted(
                prenomSaisonnier,
                nomSaisonnier,
                cin,
                nomPrenomParent,
                matriculeParent,
                utilise,
                autorises,
                prenom,
                nom,
                directionRH,
                commentaire != null ? commentaire : "-"
        );

        for (String email : emailsAdmins) {
            sendEmail(email, sujet, corps);
        }
    }

    public void sendSaisonnierWelcomeEmail(String to, String nom,
                                           String motDePasse, String token) {

        String lienVerification =
               "https://saisonnier.tunisietelecom.tn/auth/verify?token=" + token;
        String contenu = """
    	Bonjour %s,

    	Votre candidature a été envoyée avec succès.

    	Vous pouvez maintenant vous connecter à la plateforme via  le lien suivant : https://saisonnier.tunisietelecom.tn/saisonnier/login

    	── Identifiants ──
 	Email : %s
	Mot de passe : %s

	Cordialement
	Equipe DDRH
    """.formatted(
                nom,
                to,
                motDePasse,
                lienVerification
        );

        sendEmail(
                to,
                "🎉 Bienvenue — Activation de votre compte saisonnier",
                contenu
        );
    }
}
