package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.RoleRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.dto.UserRequestDto;
import com.ibizabroker.bibliotheque.dto.UserResponseDto;
import com.ibizabroker.bibliotheque.entity.Role;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.BusinessRuleException;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Regles de gestion des comptes.
 *
 * Cette couche n'existait pas : AdminController parlait directement au
 * repository, hachait le mot de passe lui-meme et enregistrait le corps de la
 * requete tel quel. Le controleur ne fait plus que traduire en HTTP.
 */
@Service
public class UsersService {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponseDto> lister() {
        return usersRepository.findAll().stream()
                .map(UserResponseDto::de)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponseDto consulter(Integer id) {
        return UserResponseDto.de(trouver(id));
    }

    @Transactional
    public UserResponseDto creer(UserRequestDto demande) {
        // Le controle explicite d'unicite precede l'insertion pour que le refus
        // porte un message utile. Sans lui, la contrainte de la base remonterait
        // en DataIntegrityViolationException, dont le texte nomme un index et
        // pas le champ fautif.
        if (usersRepository.findByUsername(demande.getUsername()).isPresent()) {
            throw new BusinessRuleException(
                    String.format("Le nom d'utilisateur '%s' est deja pris.", demande.getUsername()));
        }

        if (!StringUtils.hasText(demande.getPassword())) {
            throw new BusinessRuleException(
                    "Le mot de passe est obligatoire a la creation d'un compte.");
        }

        Users utilisateur = new Users();
        utilisateur.setUsername(demande.getUsername());
        utilisateur.setName(demande.getName());
        utilisateur.setPassword(passwordEncoder.encode(demande.getPassword()));
        utilisateur.setRole(resoudreRoles(demande.getRoleIds()));

        return UserResponseDto.de(usersRepository.save(utilisateur));
    }

    @Transactional
    public UserResponseDto modifier(Integer id, UserRequestDto demande) {
        Users utilisateur = trouver(id);

        // Changer de nom d'utilisateur est permis, mais pas pour prendre celui
        // d'un autre compte.
        boolean nomDejaPris = usersRepository.findByUsername(demande.getUsername())
                .filter(autre -> !autre.getUserId().equals(id))
                .isPresent();
        if (nomDejaPris) {
            throw new BusinessRuleException(
                    String.format("Le nom d'utilisateur '%s' est deja pris.", demande.getUsername()));
        }

        utilisateur.setUsername(demande.getUsername());
        utilisateur.setName(demande.getName());
        utilisateur.setRole(resoudreRoles(demande.getRoleIds()));

        // Un mot de passe absent laisse l'ancien en place. Le controleur ne
        // pouvait pas exprimer cette nuance : il ne touchait jamais au mot de
        // passe, et il etait donc impossible d'en changer.
        if (StringUtils.hasText(demande.getPassword())) {
            utilisateur.setPassword(passwordEncoder.encode(demande.getPassword()));
        }

        return UserResponseDto.de(usersRepository.save(utilisateur));
    }

    @Transactional
    public void supprimer(Integer id) {
        Users utilisateur = trouver(id);

        // Le dernier administrateur ne peut pas etre supprime : plus personne ne
        // pourrait alors administrer l'application, et aucune interface ne
        // permettrait de revenir en arriere.
        boolean estAdmin = utilisateur.getRole() != null && utilisateur.getRole().stream()
                .anyMatch(r -> "Admin".equalsIgnoreCase(r.getRoleName()));

        if (estAdmin && compterAdministrateurs() <= 1) {
            throw new BusinessRuleException(
                    "Ce compte est le dernier administrateur : le supprimer rendrait "
                            + "l'application inadministrable.");
        }

        usersRepository.delete(utilisateur);
    }

    private long compterAdministrateurs() {
        return usersRepository.findAll().stream()
                .filter(u -> u.getRole() != null && u.getRole().stream()
                        .anyMatch(r -> "Admin".equalsIgnoreCase(r.getRoleName())))
                .count();
    }

    private Users trouver(Integer id) {
        return usersRepository.findById(id).orElseThrow(() -> new NotFoundException(
                String.format("Le compte d'identifiant %d n'existe pas.", id)));
    }

    /**
     * Relit chaque role depuis la base.
     *
     * C'est le coeur de la correction : un role venu du JSON est une entite
     * detachee que Hibernate refuse de persister, et rien n'empechait un client
     * d'en inventer un. Un identifiant inconnu vaut desormais 404, pas 500.
     */
    private Set<Role> resoudreRoles(Set<Integer> identifiants) {
        Set<Role> roles = new LinkedHashSet<>();
        for (Integer identifiant : identifiants) {
            roles.add(roleRepository.findById(identifiant).orElseThrow(
                    () -> new NotFoundException(
                            String.format("Le role d'identifiant %d n'existe pas.", identifiant))));
        }
        return roles;
    }
}
