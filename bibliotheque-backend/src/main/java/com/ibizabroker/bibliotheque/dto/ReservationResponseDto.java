package com.ibizabroker.bibliotheque.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ibizabroker.bibliotheque.entity.StatutReservation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Ce que le client recoit. Les relations sont aplaties en identifiant + libelle
 * pour deux raisons : ne pas exposer la structure interne des entites Books et
 * Users, et couper la serialisation en cascade (Users porte ses roles, qui
 * porteraient leurs propres champs).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ReservationResponse", description = "Reservation telle que renvoyee par l'API")
public class ReservationResponseDto {

    @Schema(description = "Identifiant de la reservation", example = "7")
    private Integer id;

    @Schema(description = "Identifiant du livre reserve", example = "1")
    private Integer livreId;

    @Schema(description = "Titre du livre reserve", example = "Le Petit Prince")
    private String livreTitre;

    @Schema(description = "Identifiant de l'adherent", example = "2")
    private Integer adherentId;

    @Schema(description = "Nom de l'adherent", example = "Jean Dupont")
    private String adherentNom;

    /**
     * Format impose explicitement plutot que laisse au defaut de Jackson : sans
     * cela, la sortie depend de la configuration de WRITE_DATES_AS_TIMESTAMPS et
     * peut basculer d'une chaine ISO a un tableau [2026,8,21,10,15].
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Horodatage serveur de la prise de reservation", example = "2026-08-21T10:15:00")
    private LocalDateTime dateReservation;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Echeance : dateReservation + 7 jours (RG-04)", example = "2026-08-28T10:15:00")
    private LocalDateTime dateExpiration;

    @Schema(description = "Statut courant de la reservation", example = "EN_ATTENTE")
    private StatutReservation statut;
}
