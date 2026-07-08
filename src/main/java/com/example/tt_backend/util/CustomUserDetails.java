package com.example.tt_backend.util;

import com.example.tt_backend.entity.Utilisateur;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    // ❌ ne pas garder l'entité complète
    private final Long id;
    private final String email;
    private final String password;
    private final String role;
    private final String nom;
    private final String prenom;
    private final Boolean enabled;

    public CustomUserDetails(Utilisateur utilisateur) {
        this.id = utilisateur.getId();
        this.email = utilisateur.getEmail();
        this.password = utilisateur.getPassword();
        this.role = utilisateur.getRole().name();
        this.nom = utilisateur.getNom();
        this.prenom = utilisateur.getPrenom();
        this.enabled = utilisateur.getEnabled();
    }

    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }
}