package com.ibizabroker.bibliotheque.configuration;

import com.ibizabroker.bibliotheque.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Bonus : fait passer en EXPIREE les reservations dont l'echeance est depassee.
 *
 * @EnableScheduling est porte par cette classe plutot que par
 * BibliothequeApplication : la planification n'existe que pour ce besoin, et la
 * retirer se resume a supprimer ce fichier.
 *
 * La periode est configurable sans modifier application.properties grace a la
 * valeur par defaut inscrite dans l'expression : une heure, soit une precision
 * tres suffisante pour une echeance exprimee en jours.
 */
@Configuration
@EnableScheduling
public class ReservationExpirationJob {

    @Autowired
    private ReservationService reservationService;

    /**
     * fixedDelay et non fixedRate : le delai court a partir de la fin de
     * l'execution precedente. Si un balayage prend anormalement longtemps, les
     * executions ne s'empilent pas.
     */
    @Scheduled(
            initialDelayString = "${bibliotheque.reservation.expiration.delai-initial-ms:60000}",
            fixedDelayString = "${bibliotheque.reservation.expiration.periode-ms:3600000}")
    public void expirerLesReservationsEchues() {
        reservationService.expirerReservationsEchues();
    }
}
