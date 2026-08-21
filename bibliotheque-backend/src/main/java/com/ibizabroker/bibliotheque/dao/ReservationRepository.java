package com.ibizabroker.bibliotheque.dao;

import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.StatutReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Acces aux reservations.
 *
 * Les noms de methodes traversent les relations : « AdherentUserId » designe
 * reservation.adherent.userId, « LivreBookId » designe reservation.livre.bookId.
 * Spring Data construit le JOIN a partir de ces chemins, aucune requete n'est
 * ecrite a la main.
 */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Integer> {

    /**
     * RG-02 : existe-t-il deja une reservation active de cet adherent sur ce livre ?
     * exists... et non find... : on ne veut savoir que oui ou non, inutile de
     * charger les lignes pour les compter ensuite.
     */
    boolean existsByAdherentUserIdAndLivreBookIdAndStatutIn(
            Integer adherentId, Integer livreId, Collection<StatutReservation> statuts);

    /** RG-03 : combien de reservations actives cet adherent detient-il ? */
    long countByAdherentUserIdAndStatutIn(
            Integer adherentId, Collection<StatutReservation> statuts);

    List<Reservation> findAllByOrderByDateReservationDesc();

    List<Reservation> findByStatutOrderByDateReservationDesc(StatutReservation statut);

    List<Reservation> findByAdherentUserIdOrderByDateReservationDesc(Integer adherentId);

    List<Reservation> findByAdherentUserIdAndStatutOrderByDateReservationDesc(
            Integer adherentId, StatutReservation statut);

    /**
     * Reservations actives dont l'echeance est depassee : la cible du passage
     * automatique en EXPIREE. Le filtre sur les statuts actifs applique RG-06,
     * en excluant d'office les reservations deja terminales.
     */
    List<Reservation> findByStatutInAndDateExpirationBefore(
            Collection<StatutReservation> statuts, LocalDateTime instant);
}
