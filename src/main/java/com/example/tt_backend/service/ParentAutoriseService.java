package com.example.tt_backend.service;

import com.example.tt_backend.entity.ParentAutorise;
import com.example.tt_backend.repository.ParentAutoriseRepository;
import org.springframework.stereotype.Service;

import java.util.List;


import java.util.NoSuchElementException;

@Service
public class ParentAutoriseService {

    private final ParentAutoriseRepository parentRepo;

    public ParentAutoriseService(ParentAutoriseRepository parentRepo) {
        this.parentRepo = parentRepo;
    }

    public List<ParentAutorise> getParentsByCampagne(Long campagneId) {
        return parentRepo.findByCampagneId(campagneId);
    }

    public List<ParentAutorise> getAllParents() {
        return parentRepo.findAll();
    }

    public ParentAutorise getParentById(Long id) {
        return parentRepo.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Parent non trouvé"));
    }

    public ParentAutorise addParent(String nomPrenom, String matricule, String email, int autorises) {

        if (parentRepo.existsByMatricule(matricule)) {
            throw new IllegalArgumentException("Matricule déjà existant");
        }

        ParentAutorise parent = new ParentAutorise();
        parent.setNomPrenom(nomPrenom);
        parent.setMatricule(matricule);
	parent.setEmail(email);
        parent.setAutorises(autorises);
        parent.setUtilise(0);

        return parentRepo.save(parent);
    }

    public ParentAutorise updateParent(Long id, String nomPrenom,
                                       String matricule, String email,  int autorises, int utilise) {

        ParentAutorise parent = parentRepo.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Parent non trouvé"));

        if (!parent.getMatricule().equals(matricule)
                && parentRepo.existsByMatricule(matricule)) {
            throw new IllegalArgumentException("Matricule déjà utilisé");
        }

        parent.setNomPrenom(nomPrenom);
        parent.setMatricule(matricule);
	parent.setEmail(email);
        parent.setAutorises(autorises);
        parent.setUtilise(utilise);

        return parentRepo.save(parent);
    }

    public void deleteParent(Long id) {
        parentRepo.deleteById(id);
    }
}
