package com.ibizabroker.bibliotheque.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Borrow;
import com.ibizabroker.bibliotheque.entity.Users;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Date;

/**
 * Un emprunt tel que l'API le renvoie.
 *
 * L'entite Borrow ne portait que des identifiants : afficher « qui a emprunte
 * quoi » obligeait le client a rappeler l'API livre par livre et compte par
 * compte. Le titre et le nom sont resolus ici, en une seule reponse.
 */
@Getter
@AllArgsConstructor
@Schema(name = "BorrowResponse", description = "Emprunt, avec le titre et l'emprunteur resolus")
public class BorrowResponseDto {

    @Schema(description = "Identifiant de l'emprunt", example = "5")
    private Integer borrowId;

    @Schema(description = "Identifiant du livre", example = "202")
    private Integer bookId;

    @Schema(description = "Titre du livre", example = "L2 - Germinal")
    private String bookName;

    @Schema(description = "Identifiant de l'emprunteur", example = "303")
    private Integer userId;

    @Schema(description = "Nom de l'emprunteur", example = "A3 - Emprunteur de L2 a L5")
    private String userName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Date d'emprunt")
    private Date issueDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Echeance de restitution")
    private Date dueDate;

    /** Null tant que le livre n'est pas rendu : c'est ce qui definit « en cours ». */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Date de restitution, absente si l'emprunt est en cours")
    private Date returnDate;

    @Schema(description = "Vrai si l'echeance est depassee et le livre non rendu")
    private boolean enRetard;

    public static BorrowResponseDto de(Borrow emprunt, Books livre, Users emprunteur) {
        boolean enRetard = emprunt.getReturnDate() == null
                && emprunt.getDueDate() != null
                && emprunt.getDueDate().before(new Date());

        return new BorrowResponseDto(
                emprunt.getBorrowId(),
                emprunt.getBookId(),
                livre == null ? null : livre.getBookName(),
                emprunt.getUserId(),
                emprunteur == null ? null : emprunteur.getName(),
                emprunt.getIssueDate(),
                emprunt.getDueDate(),
                emprunt.getReturnDate(),
                enRetard);
    }
}
