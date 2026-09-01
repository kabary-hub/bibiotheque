package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dto.ReservationRequestDto;
import com.ibizabroker.bibliotheque.dto.ReservationResponseDto;
import com.ibizabroker.bibliotheque.entity.StatutReservation;
import com.ibizabroker.bibliotheque.exceptions.ApiError;
import com.ibizabroker.bibliotheque.service.ReservationService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * Exposition HTTP du module Reservation.
 *
 * Aucune logique metier ici : chaque methode valide son entree, delegue au
 * service et traduit le retour en code HTTP. Les erreurs ne sont pas attrapees
 * non plus, ReservationExceptionHandler s'en charge.
 */
@CrossOrigin("http://localhost:4200")
@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservations", description = "Reservation d'un livre indisponible par un adherent")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @Operation(
            summary = "Creer une reservation",
            description = "Le client n'envoie que livreId et adherentId. La date de reservation, "
                    + "la date d'expiration (RG-04) et le statut initial EN_ATTENTE sont determines "
                    + "par le serveur. Controles appliques : RG-01, RG-02, RG-03.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reservation creee"),
            @ApiResponse(responseCode = "400", description = "livreId ou adherentId manquant",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Livre ou adherent inconnu",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Regle de gestion enfreinte (RG-01, RG-02 ou RG-03)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<ReservationResponseDto> creer(
            @Valid @RequestBody ReservationRequestDto demande) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.creer(demande));
    }

    @Operation(
            summary = "Lister les reservations",
            description = "Sans parametre, renvoie toutes les reservations. Les deux filtres sont "
                    + "facultatifs et combinables.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des reservations"),
            @ApiResponse(responseCode = "400", description = "Valeur de statut inconnue",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public List<ReservationResponseDto> lister(
            @Parameter(description = "Filtre sur le statut")
            @RequestParam(required = false) StatutReservation statut,
            @Parameter(description = "Filtre sur l'identifiant de l'adherent", example = "2")
            @RequestParam(required = false) Integer adherentId) {
        return reservationService.lister(statut, adherentId);
    }

    /**
     * Declare avant /{id} par lisibilite. L'ordre n'a pas d'incidence
     * fonctionnelle : Spring fait primer un chemin litteral sur une variable.
     */
    @Operation(
            summary = "Lister les reservations expirees",
            description = "Bonus : raccourci equivalent a GET /api/reservations?statut=EXPIREE.")
    @ApiResponse(responseCode = "200", description = "Liste des reservations expirees")
    @GetMapping("/expirees")
    public List<ReservationResponseDto> listerExpirees() {
        return reservationService.listerExpirees();
    }

    @Operation(summary = "Consulter une reservation")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservation trouvee"),
            @ApiResponse(responseCode = "404", description = "Reservation inconnue",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public ReservationResponseDto consulter(
            @Parameter(description = "Identifiant de la reservation", example = "1")
            @PathVariable Integer id) {
        return reservationService.consulter(id);
    }

    @Operation(
            summary = "Annuler une reservation",
            description = "RG-05 : seule une reservation EN_ATTENTE ou DISPONIBLE peut etre annulee. "
                    + "RG-06 : un statut terminal (ANNULEE, EXPIREE, HONOREE) est definitif.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservation annulee"),
            @ApiResponse(responseCode = "404", description = "Reservation inconnue",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Statut incompatible avec une annulation (RG-05)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("/{id}/annuler")
    public ReservationResponseDto annuler(
            @Parameter(description = "Identifiant de la reservation", example = "1")
            @PathVariable Integer id) {
        return reservationService.annuler(id);
    }

    @Operation(
            summary = "Supprimer une reservation",
            description = "Suppression definitive de la ligne. Pour retirer une reservation du "
                    + "circuit en conservant sa trace, utiliser l'annulation.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reservation supprimee"),
            @ApiResponse(responseCode = "404", description = "Reservation inconnue",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(
            @Parameter(description = "Identifiant de la reservation", example = "1")
            @PathVariable Integer id) {
        reservationService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
