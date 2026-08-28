package com.ibizabroker.bibliotheque.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * Ce que le client a le droit d'envoyer : deux identifiants, rien de plus.
 *
 * La date de reservation, la date d'expiration et le statut sont determines
 * par le serveur. Les absenter de ce DTO est la seule facon fiable de garantir
 * qu'un client ne peut pas les imposer : un @RequestBody Reservation aurait
 * accepte n'importe quel statut envoye a la main.
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

    @NotNull(message = "Le champ 'adherentId' est obligatoire.")
    @Schema(description = "Identifiant de l'adherent qui reserve", example = "2", required = true)
    private Integer adherentId;
}
