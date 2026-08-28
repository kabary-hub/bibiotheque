package com.ibizabroker.bibliotheque.dto;

import com.ibizabroker.bibliotheque.entity.Role;
import com.ibizabroker.bibliotheque.entity.Users;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Un compte tel que l'API le renvoie.
 *
 * L'entite Users etait serialisee telle quelle : le champ « password » — le
 * hachage BCrypt — partait dans le corps de CHAQUE reponse, y compris la liste
 * complete des comptes de GET /admin/users. Un hachage n'est pas un mot de passe
 * en clair, mais c'est de la matiere a attaque hors ligne, et rien dans
 * l'interface n'en avait l'usage.
 *
 * Ce DTO n'a pas de champ password. L'omission n'est pas un oubli a corriger.
 */
@Getter
@AllArgsConstructor
@Schema(name = "UserResponse", description = "Compte utilisateur, sans element secret")
public class UserResponseDto {

    @Schema(description = "Identifiant du compte", example = "301")
    private Integer userId;

    @Schema(description = "Identifiant de connexion", example = "a1_reservataire")
    private String username;

    @Schema(description = "Nom affiche", example = "A1 - Reservataire principal")
    private String name;

    @Schema(description = "Roles accordes", example = "[\"User\"]")
    private List<String> roles;

    public static UserResponseDto de(Users utilisateur) {
        List<String> roles = utilisateur.getRole() == null
                ? Collections.emptyList()
                : utilisateur.getRole().stream()
                        .map(Role::getRoleName)
                        .sorted()
                        .collect(Collectors.toList());

        return new UserResponseDto(
                utilisateur.getUserId(),
                utilisateur.getUsername(),
                utilisateur.getName(),
                roles);
    }
}
