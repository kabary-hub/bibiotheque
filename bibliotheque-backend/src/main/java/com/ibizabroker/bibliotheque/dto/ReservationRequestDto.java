package com.ibizabroker.bibliotheque.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * Ce que le client a le droit d'envoyer : un seul identifiant de livre.
 *
 * L'identifiant de l'adherent n'est PLUS dans ce DTO (RS-04).
 * Il est recupere depuis le token JWT par le contrôleur : c'est la seule
 * facon fiable de garantir qu'un client ne peut pas creer une reservation
 * au nom de quelqu'un d'autre.
 *
 * La date de reservation, la date d'expiration et le statut sont determines
 * par le serveur.
 */
@Data
@Schema(name = "ReservationRequest", description = "Demande de reservation d'un livre")
public class ReservationRequestDto {

    /**
     * Le message est explicite parce que l'enonce l'exige : la reponse 400 doit
     * dire quel champ manque, pas seulement qu'un champ manque.
     */
    @NotNull(message = "Le champ 'livreId' est obligatoire.")
    @Schema(description = "Identifiant du livre a reserver", example = "1", required = true)
    private Integer livreId;
}
