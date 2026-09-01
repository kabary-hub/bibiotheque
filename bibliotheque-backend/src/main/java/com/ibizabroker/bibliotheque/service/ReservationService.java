package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.dto.ReservationRequestDto;
import com.ibizabroker.bibliotheque.dto.ReservationResponseDto;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.StatutReservation;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.BusinessRuleException;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Toute la logique metier du module Reservation.
 *
 * Le controleur ne fait qu'appeler ces methodes : il ne lit aucun repository,
 * n'evalue aucune regle et ne voit jamais l'entite Reservation. Les regles
 * RG-01 a RG-06 sont donc verifiables en un seul fichier, et testables sans
 * demarrer de serveur HTTP.
 */
@Service
@Transactional
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    /** RG-03 : plafond de reservations actives par adherent. */
    public static final int MAX_RESERVATIONS_ACTIVES = 3;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private BooksRepository booksRepository;

    @Autowired
    private UsersRepository usersRepository;

    // ------------------------------------------------------------------
    //  Creation
    // ------------------------------------------------------------------

    /**
     * Cree une reservation apres avoir verifie RG-01, RG-02 et RG-03.
     *
     * L'ordre des controles n'est pas arbitraire : les existences d'abord (404),
     * les regles ensuite (409). Repondre 409 sur un livre qui n'existe pas
     * serait mensonger : il n'y a pas de conflit d'etat, il y a une ressource
     * absente.
     */
    public ReservationResponseDto creer(ReservationRequestDto demande) {

        Books livre = booksRepository.findById(demande.getLivreId())
                .orElseThrow(() -> new NotFoundException(
                        "Le livre d'identifiant " + demande.getLivreId() + " n'existe pas."));

        Users adherent = usersRepository.findById(demande.getAdherentId())
                .orElseThrow(() -> new NotFoundException(
                        "L'adherent d'identifiant " + demande.getAdherentId() + " n'existe pas."));

        // --- RG-01 : on ne reserve que ce qu'on ne peut pas emprunter --------
        if (estDisponible(livre)) {
            throw new BusinessRuleException("RG-01", String.format(
                    "le livre %s est disponible (%d exemplaire(s) en rayon). Une reservation "
                            + "ne se justifie que si le livre est indisponible : empruntez-le.",
                    livre.getBookName(), livre.getNoOfCopies()));
        }

        // --- RG-02 : pas deux reservations actives sur le meme livre ---------
        boolean dejaReserve = reservationRepository.existsByAdherentUserIdAndLivreBookIdAndStatutIn(
                adherent.getUserId(), livre.getBookId(), StatutReservation.ACTIFS);
        if (dejaReserve) {
            throw new BusinessRuleException("RG-02", String.format(
                    "l'adherent %s a deja une reservation active sur le livre %s.",
                    adherent.getUsername(), livre.getBookName()));
        }

        // --- RG-03 : trois reservations actives au maximum -------------------
        long actives = reservationRepository.countByAdherentUserIdAndStatutIn(
                adherent.getUserId(), StatutReservation.ACTIFS);
        if (actives >= MAX_RESERVATIONS_ACTIVES) {
            throw new BusinessRuleException("RG-03", String.format(
                    "l'adherent %s detient deja %d reservations actives, le maximum autorise "
                            + "est de %d. Annulez-en une avant d'en creer une nouvelle.",
                    adherent.getUsername(), actives, MAX_RESERVATIONS_ACTIVES));
        }

        Reservation reservation = new Reservation();
        reservation.setLivre(livre);
        reservation.setAdherent(adherent);

        // --- RG-04 : les deux dates viennent du serveur, jamais du client ----
        // Un seul appel a now(), reutilise pour les deux dates : deux appels
        // separes donneraient un ecart de quelques microsecondes, et
        // dateExpiration ne vaudrait plus exactement dateReservation + 7 jours.
        LocalDateTime maintenant = LocalDateTime.now();
        reservation.setDateReservation(maintenant);
        reservation.setDateExpiration(maintenant.plusDays(Reservation.DUREE_VALIDITE_JOURS));

        reservation.setStatut(StatutReservation.EN_ATTENTE);

        Reservation enregistree = reservationRepository.save(reservation);
        log.info("Reservation {} creee : livre {} pour l'adherent {}, echeance {}",
                enregistree.getReservationId(), livre.getBookId(),
                adherent.getUserId(), enregistree.getDateExpiration());

        return versDto(enregistree);
    }

    // ------------------------------------------------------------------
    //  Consultation
    // ------------------------------------------------------------------

    /**
     * Liste les reservations, filtrable par statut et par adherent.
     *
     * Les deux filtres sont independants et combinables : quatre cas, quatre
     * requetes derivees. Une requete unique a parametres optionnels serait plus
     * courte a lire, mais imposerait de passer un enum null a Hibernate, ce que
     * celui-ci ne sait pas typer de facon fiable.
     */
    @Transactional(readOnly = true)
    public List<ReservationResponseDto> lister(StatutReservation statut, Integer adherentId) {

        List<Reservation> resultat;
        if (adherentId != null && statut != null) {
            resultat = reservationRepository
                    .findByAdherentUserIdAndStatutOrderByDateReservationDesc(adherentId, statut);
        } else if (adherentId != null) {
            resultat = reservationRepository.findByAdherentUserIdOrderByDateReservationDesc(adherentId);
        } else if (statut != null) {
            resultat = reservationRepository.findByStatutOrderByDateReservationDesc(statut);
        } else {
            resultat = reservationRepository.findAllByOrderByDateReservationDesc();
        }
        return versDtos(resultat);
    }

    @Transactional(readOnly = true)
    public ReservationResponseDto consulter(Integer id) {
        return versDto(chercher(id));
    }

    /** Bonus : les reservations dont l'echeance a ete constatee depassee. */
    @Transactional(readOnly = true)
    public List<ReservationResponseDto> listerExpirees() {
        return versDtos(reservationRepository
                .findByStatutOrderByDateReservationDesc(StatutReservation.EXPIREE));
    }

    // ------------------------------------------------------------------
    //  Transitions d'etat
    // ------------------------------------------------------------------

    /**
     * Annule une reservation.
     *
     * RG-05 n'autorise l'annulation que depuis EN_ATTENTE ou DISPONIBLE, et
     * RG-06 gele les trois autres statuts. Les deux regles designent ici le
     * meme ensemble : le controle est donc unique, et le message cite les deux.
     */
    public ReservationResponseDto annuler(Integer id) {

        Reservation reservation = chercher(id);

        if (!reservation.getStatut().estActif()) {
            throw new BusinessRuleException("RG-05", String.format(
                    "la reservation %d est au statut %s. Seule une reservation EN_ATTENTE ou "
                            + "DISPONIBLE peut etre annulee, et RG-06 rend un statut terminal definitif.",
                    id, reservation.getStatut()));
        }

        reservation.setStatut(StatutReservation.ANNULEE);
        log.info("Reservation {} annulee.", id);
        return versDto(reservationRepository.save(reservation));
    }

    public void supprimer(Integer id) {
        reservationRepository.delete(chercher(id));
        log.info("Reservation {} supprimee.", id);
    }

    /**
     * Bonus : bascule en EXPIREE les reservations actives dont l'echeance est
     * passee. Declenchee periodiquement par ReservationExpirationJob.
     *
     * Le filtre porte sur StatutReservation.ACTIFS : c'est ainsi que RG-06 est
     * respectee ici. Une reservation ANNULEE ou HONOREE dont la date est
     * depassee n'est pas retouchee, un statut terminal ne changeant plus, meme
     * sous l'effet du temps.
     *
     * @return le nombre de reservations basculees
     */
    public int expirerReservationsEchues() {

        List<Reservation> echues = reservationRepository.findByStatutInAndDateExpirationBefore(
                StatutReservation.ACTIFS, LocalDateTime.now());

        for (Reservation reservation : echues) {
            reservation.setStatut(StatutReservation.EXPIREE);
        }
        reservationRepository.saveAll(echues);

        if (!echues.isEmpty()) {
            log.info("{} reservation(s) passee(s) en EXPIREE.", echues.size());
        }
        return echues.size();
    }

    // ------------------------------------------------------------------
    //  Interne
    // ------------------------------------------------------------------

    private Reservation chercher(Integer id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "La reservation d'identifiant " + id + " n'existe pas."));
    }

    /**
     * RG-01, cote lecture. Un noOfCopies null est traite comme une
     * indisponibilite : une donnee absente ne doit pas valoir « disponible »
     * par accident.
     */
    private boolean estDisponible(Books livre) {
        return livre.getNoOfCopies() != null && livre.getNoOfCopies() > 0;
    }

    /**
     * Seul point de sortie de l'entite. La conversion vit dans le service, et
     * non dans le DTO ni dans le controleur : c'est ce qui garantit que
     * Reservation ne franchit jamais cette frontiere.
     */
    private ReservationResponseDto versDto(Reservation reservation) {
        return new ReservationResponseDto(
                reservation.getReservationId(),
                reservation.getLivre().getBookId(),
                reservation.getLivre().getBookName(),
                reservation.getAdherent().getUserId(),
                reservation.getAdherent().getName(),
                reservation.getDateReservation(),
                reservation.getDateExpiration(),
                reservation.getStatut());
    }

    private List<ReservationResponseDto> versDtos(List<Reservation> reservations) {
        return reservations.stream().map(this::versDto).collect(Collectors.toList());
    }
}
