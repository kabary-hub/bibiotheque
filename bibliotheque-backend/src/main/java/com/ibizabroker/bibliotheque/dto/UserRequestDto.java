package com.ibizabroker.bibliotheque.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.Set;

/**
 * Ce qu'un client a le droit d'envoyer pour creer ou modifier un compte.
 *
 * L'entite Users etait recue directement en @RequestBody. Trois consequences :
 * le client pouvait imposer son propre userId, envoyer des roles inventes, et
 * rien n'etait valide — un nom d'utilisateur vide passait jusqu'a la base.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(name = "UserRequest", description = "Donnees de creation ou de modification d'un compte")
public class UserRequestDto {

    @NotBlank(message = "Le nom d'utilisateur est obligatoire.")
    @Size(min = 3, max = 50, message = "Le nom d'utilisateur doit compter entre 3 et 50 caracteres.")
    @Schema(description = "Identifiant de connexion, unique", example = "jdupont")
    private String username;

    @NotBlank(message = "Le nom complet est obligatoire.")
    @Size(max = 120, message = "Le nom complet ne peut pas depasser 120 caracteres.")
    @Schema(description = "Nom affiche", example = "Jean Dupont")
    private String name;

    /**
     * Facultatif a la modification : ne pas l'envoyer laisse le mot de passe en
     * place. L'exiger obligerait a le ressaisir pour changer un simple nom.
     */
    @Size(min = 6, max = 72, message = "Le mot de passe doit compter au moins 6 caracteres.")
    @Schema(description = "Mot de passe en clair, hache par le serveur. Absent = inchange.")
    private String password;

    @NotEmpty(message = "Au moins un role est obligatoire.")
    @Schema(description = "Identifiants des roles a accorder. 1 = Admin, 2 = User.", example = "[2]")
    private Set<Integer> roleIds;
}
