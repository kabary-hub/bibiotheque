package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dto.ReservationRequestDto;
import com.ibizabroker.bibliotheque.dto.ReservationResponseDto;
import com.ibizabroker.bibliotheque.entity.StatutReservation;
import com.ibizabroker.bibliotheque.entity.Users;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
 * Exposition HTTP du module Reservation, securisee (RS-01 a RS-05).
 *
 * Aucune logique metier ici : chaque methode valide son entree, recupere
 * l'utilisateur courant depuis le SecurityContext, delegue au service
 * et traduit le retour en code HTTP.
 *
 * RS-04 : l'identite de l'adherent vient toujours du token JWT,
 * jamais du corps de la requete.
 */
@CrossOrigin("http://localhost:4200")
@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservations", description = "Reservation d'un livre indisponible par un adherent")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    // ------------------------------------------------------------------
    //  POST — creer une reservation
    // ------------------------------------------------------------------

    /**
     * RS-04 : l'identifiant de l'adherent n'est plus dans le corps de la
     * requete. Il est recupere depuis le token JWT via SecurityContextHolder.
     * Un client ne peut jamais creer une reservation au nom d'un autre.
     */
    @Operation(
            summary = "Creer une reservation",
            description = "Le client n'envoie que livreId. L'identifiant de l'adherent est recupere "
                    + "depuis le token JWT (RS-04). Les dates et le statut sont determines par le serveur. "
                    + "Controles appliques : RG-01, RG-02, RG-03.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reservation creee"),
            @ApiResponse(responseCode = "400", description = "livreId manquant",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Livre ou adherent inconnu",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Regle de gestion enfreinte (RG-01, RG-02 ou RG-03)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Token absent ou invalide")
    })
    @PostMapping
    public ResponseEntity<ReservationResponseDto> creer(
            @Valid @RequestBody ReservationRequestDto demande) {

        Users adherent = recupererUtilisateurCourant();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationService.creer(demande, adherent));
    }

    // ------------------------------------------------------------------
    //  GET — lister les reservations (RS-05)
    // ------------------------------------------------------------------

    /**
     * RS-05 : un ADHERENT ne voit que ses propres reservations.
     * Un BIBLIOTHECAIRE voit toutes les reservations.
     */
    @Operation(
            summary = "Lister les reservations",
            description = "RS-05 : un ADHERENT ne voit que ses propres reservations. "
                    + "Un BIBLIOTHECAIRE voit toutes les reservations. "
                    + "Les deux filtres sont facultatifs et combinables.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des reservations"),
            @ApiResponse(responseCode = "400", description = "Valeur de statut inconnue",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Token absent ou invalide")
    })
    @GetMapping
    public List<ReservationResponseDto> lister(
            @Parameter(description = "Filtre sur le statut")
            @RequestParam(required = false) StatutReservation statut,
            @Parameter(description = "Filtre sur l'identifiant de l'adherent (admin uniquement)", example = "2")
            @RequestParam(required = false) Integer adherentId) {

        Users adherent = recupererUtilisateurCourant();
        boolean estBibliothecaire = aLeRole("BIBLIOTHECAIRE");
        return reservationService.lister(statut, adherentId, adherent, estBibliothecaire);
    }

    /**
     * Declare avant /{id} par lisibilite.
     */
    @Operation(
            summary = "Lister les reservations expirees",
            description = "Bonus : raccourci equivalent a GET /api/reservations?statut=EXPIREE.")
    @ApiResponse(responseCode = "200", description = "Liste des reservations expirees")
    @GetMapping("/expirees")
    public List<ReservationResponseDto> listerExpirees() {
        return reservationService.listerExpirees();
    }

    // ------------------------------------------------------------------
    //  GET /{id} — consulter (RS-03)
    // ------------------------------------------------------------------

    /**
     * RS-03 : un ADHERENT ne peut consulter qu'une reservation qui lui appartient.
     */
    @Operation(summary = "Consulter une reservation")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservation trouvee"),
            @ApiResponse(responseCode = "404", description = "Reservation inconnue",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Acces refuse (RS-03 : reservation d'un autre adherent)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Token absent ou invalide")
    })
    @GetMapping("/{id}")
    public ReservationResponseDto consulter(
            @Parameter(description = "Identifiant de la reservation", example = "1")
            @PathVariable Integer id) {

        Users adherent = recupererUtilisateurCourant();
        boolean estBibliothecaire = aLeRole("BIBLIOTHECAIRE");
        return reservationService.consulter(id, adherent, estBibliothecaire);
    }

    // ------------------------------------------------------------------
    //  PATCH /{id}/annuler — annuler (RS-03)
    // ------------------------------------------------------------------

    /**
     * RS-03 : un ADHERENT ne peut annuler qu'une reservation qui lui appartient.
     */
    @Operation(
            summary = "Annuler une reservation",
            description = "RG-05 : seule une reservation EN_ATTENTE ou DISPONIBLE peut etre annulee. "
                    + "RG-06 : un statut terminal (ANNULEE, EXPIREE, HONOREE) est definitif.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservation annulee"),
            @ApiResponse(responseCode = "404", description = "Reservation inconnue",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Statut incompatible avec une annulation (RG-05)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Acces refuse (RS-03)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Token absent ou invalide")
    })
    @PatchMapping("/{id}/annuler")
    public ReservationResponseDto annuler(
            @Parameter(description = "Identifiant de la reservation", example = "1")
            @PathVariable Integer id) {

        Users adherent = recupererUtilisateurCourant();
        boolean estBibliothecaire = aLeRole("BIBLIOTHECAIRE");
        return reservationService.annuler(id, adherent, estBibliothecaire);
    }

    // ------------------------------------------------------------------
    //  DELETE /{id} — supprimer (RS-02 : BIBLIOTHECAIRE uniquement)
    // ------------------------------------------------------------------

    /**
     * RS-02 : seul un BIBLIOTHECAIRE peut supprimer une reservation.
     * Un ADHERENT qui tente un DELETE recoit 403.
     */
    @Operation(
            summary = "Supprimer une reservation",
            description = "RS-02 : operation reservee au BIBLIOTHECAIRE. "
                    + "Suppression definitive de la ligne. Pour retirer une reservation du "
                    + "circuit en conservant sa trace, utiliser l'annulation.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reservation supprimee"),
            @ApiResponse(responseCode = "404", description = "Reservation inconnue",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Acces refuse (RS-02 : role BIBLIOTHECAIRE requis)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Token absent ou invalide")
    })
    @PreAuthorize("hasRole('BIBLIOTHECAIRE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(
            @Parameter(description = "Identifiant de la reservation", example = "1")
            @PathVariable Integer id) {
        reservationService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------

    /**
     * Recupere l'utilisateur authentifie depuis le SecurityContext.
     * Le SecurityContextHolder est la seule source fiable de l'identite
     * de l'utilisateur : c'est le filtre JWT qui y place l'objet.
     *
     * @return l'entite Users chargee depuis la base
     * @throws IllegalStateException si aucun utilisateur n'est authentifie
     */
    private Users recupererUtilisateurCourant() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Aucun utilisateur authentifie.");
        }

        String username = authentication.getName();
        // Le username est charge par JwtService.loadUserByUsername(),
        // qui recherche l'utilisateur en base via UsersRepository.
        // On utilise directement le repository pour retrouver l'entite Users.
        return usersRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException(
                        "Utilisateur '" + username + "' non trouve dans la base."));
    }

    /**
     * Verifie si l'utilisateur courant possede le role specifie.
     *
     * @param role nom du role sans le prefixe ROLE_ (ex: "BIBLIOTHECAIRE")
     * @return true si le role est present dans les autorites
     */
    private boolean aLeRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    /**
     * Injection du repository pour la methode recupererUtilisateurCourant().
     * Le controller n'a pas besoin de ce repository pour autre chose.
     */
    @Autowired
    private com.ibizabroker.bibliotheque.dao.UsersRepository usersRepository;
}
