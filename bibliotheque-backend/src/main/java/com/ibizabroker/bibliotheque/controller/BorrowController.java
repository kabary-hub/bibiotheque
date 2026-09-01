package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dto.BorrowRequestDto;
import com.ibizabroker.bibliotheque.dto.BorrowResponseDto;
import com.ibizabroker.bibliotheque.exceptions.ApiError;
import com.ibizabroker.bibliotheque.service.BorrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * Emprunts et restitutions.
 *
 * L'annotation @Repository qui decorait cette classe a ete retiree : elle
 * declare un composant d'acces aux donnees, ce qu'un controleur n'est pas. Elle
 * activait au passage la traduction des exceptions de persistance sur une
 * couche qui n'en leve aucune.
 */
@CrossOrigin("http://localhost:4200")
@RestController
@RequestMapping("/borrow")
@Tag(name = "Emprunts", description = "Pret et restitution des exemplaires")
public class BorrowController {

    @Autowired
    private BorrowService borrowService;

    @Operation(summary = "Emprunter un exemplaire",
            description = "Retire un exemplaire du rayon et pose une echeance a 7 jours.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Emprunt enregistre"),
            @ApiResponse(responseCode = "400", description = "bookId ou userId absent",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Livre ou adherent inconnu",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Plus aucun exemplaire disponible",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<BorrowResponseDto> emprunter(
            @Valid @RequestBody BorrowRequestDto demande) {
        return ResponseEntity.status(HttpStatus.CREATED).body(borrowService.emprunter(demande));
    }

    @Operation(summary = "Lister tous les emprunts")
    @ApiResponse(responseCode = "200", description = "Liste des emprunts")
    @GetMapping
    public List<BorrowResponseDto> lister() {
        return borrowService.lister();
    }

    @Operation(summary = "Restituer un exemplaire",
            description = "Remet l'exemplaire en rayon. Un emprunt deja rendu est refuse.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Restitution enregistree"),
            @ApiResponse(responseCode = "404", description = "Emprunt inconnu",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Emprunt deja restitue",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{borrowId}/restituer")
    public BorrowResponseDto restituer(
            @Parameter(description = "Identifiant de l'emprunt", example = "5")
            @PathVariable Integer borrowId) {
        return borrowService.restituer(borrowId);
    }

    @Operation(summary = "Emprunts d'un adherent")
    @ApiResponse(responseCode = "200", description = "Liste des emprunts de l'adherent")
    @GetMapping("/user/{id}")
    public List<BorrowResponseDto> parAdherent(
            @Parameter(description = "Identifiant de l'adherent", example = "303")
            @PathVariable Integer id) {
        return borrowService.listerParAdherent(id);
    }

    @Operation(summary = "Historique d'un livre")
    @ApiResponse(responseCode = "200", description = "Emprunts successifs du livre")
    @GetMapping("/book/{id}")
    public List<BorrowResponseDto> parLivre(
            @Parameter(description = "Identifiant du livre", example = "202")
            @PathVariable Integer id) {
        return borrowService.listerParLivre(id);
    }
}
