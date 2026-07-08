package com.example.tt_backend.service;


import com.example.tt_backend.entity.StatutCampagne;
import com.example.tt_backend.entity.Utilisateur;
import com.example.tt_backend.repository.UtilisateurRepository;
import com.example.tt_backend.util.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UtilisateurRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        List<Utilisateur> users = userRepository.findAllByEmail(email);

        if (users.isEmpty()) {
            throw new UsernameNotFoundException("Utilisateur non trouvé : " + email);
        }

        Utilisateur utilisateur;

        if (users.size() > 1) {
            // Plusieurs comptes avec le même email → prendre celui de la campagne ACTIVE
            utilisateur = users.stream()
                    .filter(u -> u.getCampagne() != null
                            && u.getCampagne().getStatut() == StatutCampagne.ACTIVE)
                    .findFirst()
                    .orElse(users.get(0));
        } else {
            utilisateur = users.get(0);
        }

        return new CustomUserDetails(utilisateur);
    }
}
