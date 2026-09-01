package com.ibizabroker.bibliotheque.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

/**
 * Demande d'emprunt.
 *
 * Le client n'envoie que les deux identifiants. Les dates d'emission et
 * d'echeance etaient auparavant acceptees depuis le corps de la requete puis
 * ecrasees par le serveur — un client pouvait croire les avoir choisies. Elles
 * ne figurent plus ici : ce que le serveur decide, le client ne l'envoie pas.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(name = "BorrowRequest", description = "Demande d'emprunt d'un exemplaire")
public class BorrowRequestDto {

    @NotNull(message = "L'identifiant du livre est obligatoire.")
    @Schema(description = "Identifiant du livre emprunte", example = "201")
    private Integer bookId;

    @NotNull(message = "L'identifiant de l'adherent est obligatoire.")
    @Schema(description = "Identifiant de l'adherent emprunteur", example = "303")
    private Integer userId;
}
