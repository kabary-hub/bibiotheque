package com.ibizabroker.bibliotheque.exceptions;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Corps de reponse unique pour toutes les erreurs du module Reservation.
 *
 * Un format d'erreur stable vaut mieux qu'un format par cas : le client ecrit
 * un seul chemin de lecture, quel que soit le code renvoye.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ApiError", description = "Corps renvoye pour toute erreur du module Reservation")
public class ApiError {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Horodatage de l'erreur", example = "2026-08-21T10:15:00")
    private final LocalDateTime timestamp = LocalDateTime.now();

    @Schema(description = "Code HTTP", example = "409")
    private final int status;

    @Schema(description = "Libelle du code HTTP", example = "Conflict")
    private final String error;

    @Schema(description = "Explication lisible", example = "RG-01 : le livre 1 est disponible...")
    private final String message;

    /** Renseigne uniquement pour un 409, absent du JSON sinon. */
    @Schema(description = "Reference de la regle de gestion enfreinte", example = "RG-01")
    private final String regle;

    public ApiError(int status, String error, String message, String regle) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.regle = regle;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getRegle() {
        return regle;
    }
}
