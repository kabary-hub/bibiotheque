package com.ibizabroker.bibliotheque.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Ce qu'un client a le droit d'envoyer pour creer ou modifier un livre.
 *
 * L'entite Books etait recue directement : le client pouvait imposer son propre
 * bookId, et rien n'etait valide. Un livre sans titre, ou avec un nombre
 * d'exemplaires negatif, entrait en base sans un mot — et un nombre negatif
 * fausse RG-01, qui ne teste que « no_of_copies > 0 ».
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(name = "BookRequest", description = "Donnees de creation ou de modification d'un livre")
public class BookRequestDto {

    @NotBlank(message = "Le titre du livre est obligatoire.")
    @Size(max = 200, message = "Le titre ne peut pas depasser 200 caracteres.")
    @Schema(description = "Titre", example = "L1 - Le Comte de Monte-Cristo")
    private String bookName;

    @NotBlank(message = "L'auteur est obligatoire.")
    @Size(max = 120, message = "L'auteur ne peut pas depasser 120 caracteres.")
    @Schema(description = "Auteur", example = "Alexandre Dumas")
    private String bookAuthor;

    @NotBlank(message = "Le genre est obligatoire.")
    @Size(max = 60, message = "Le genre ne peut pas depasser 60 caracteres.")
    @Schema(description = "Genre", example = "Roman")
    private String bookGenre;

    @NotNull(message = "Le nombre d'exemplaires est obligatoire.")
    @Min(value = 0, message = "Le nombre d'exemplaires ne peut pas etre negatif.")
    @Schema(description = "Exemplaires en rayon. 0 rend le livre reservable (RG-01).", example = "3")
    private Integer noOfCopies;
}
