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
import org.springframework.security.access.AccessDeniedException;
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
 * RG-01 a RG-06 et les regles de securite RS-03, RS-04, RS-05 sont
 * verifiables en un seul fichier.
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
    //  Creation (RS-04 : identite vient du token, pas du DTO)
    // ------------------------------------------------------------------

    /**
     * Cree une reservation apres avoir verifie RG-01, RG-02 et RG-03.
     *
     * RS-04 : le parametre adherent provient du token JWT, jamais du corps
     * de la requete. Le client n'a aucun moyen d'imposer un autre identifiant.
     *
     * @param demande  contient uniquement livreId
     * @param adherent l'adherent authentifie, recupere depuis le token
     */
    public ReservationResponseDto creer(ReservationRequestDto demande, Users adherent) {

        Books livre = booksRepository.findById(demande.getLivreId())
                .orElseThrow(() -> new NotFoundException(
                        "Le livre d'identifiant " + demande.getLivreId() + " n'existe pas."));

        // L'adherent vient du token : pas besoin de le rechercher en base,
        // il est deja charge par JwtService.loadUserByUsername().
        // Mais on le recharge pour s'assurer qu'il est toujours actif.
        Users adherentActif = usersRepository.findById(adherent.getUserId())
                .orElseThrow(() -> new NotFoundException(
                        "L'adherent d'identifiant " + adherent.getUserId() + " n'existe pas."));

        // --- RG-01 : on ne reserve que ce qu'on ne peut pas emprunter --------
        if (estDisponible(livre)) {
            throw new BusinessRuleException("RG-01", String.format(
                    "le livre %s est disponible (%d exemplaire(s) en rayon). Une reservation "
                            + "ne se justifie que si le livre est indisponible : empruntez-le.",
                    livre.getBookName(), livre.getNoOfCopies()));
        }

        // --- RG-02 : pas deux reservations actives sur le meme livre ---------
        boolean dejaReserve = reservationRepository.existsByAdherentUserIdAndLivreBookIdAndStatutIn(
                adherentActif.getUserId(), livre.getBookId(), StatutReservation.ACTIFS);
        if (dejaReserve) {
            throw new BusinessRuleException("RG-02", String.format(
                    "l'adherent %s a deja une reservation active sur le livre %s.",
                    adherentActif.getUsername(), livre.getBookName()));
        }

        // --- RG-03 : trois reservations actives au maximum -------------------
        long actives = reservationRepository.countByAdherentUserIdAndStatutIn(
                adherentActif.getUserId(), StatutReservation.ACTIFS);
        if (actives >= MAX_RESERVATIONS_ACTIVES) {
            throw new BusinessRuleException("RG-03", String.format(
                    "l'adherent %s detient deja %d reservations actives, le maximum autorise "
                            + "est de %d. Annulez-en une avant d'en creer une nouvelle.",
                    adherentActif.getUsername(), actives, MAX_RESERVATIONS_ACTIVES));
        }

        Reservation reservation = new Reservation();
        reservation.setLivre(livre);
        reservation.setAdherent(adherentActif);

        // --- RG-04 : les deux dates viennent du serveur, jamais du client ----
        LocalDateTime maintenant = LocalDateTime.now();
        reservation.setDateReservation(maintenant);
        reservation.setDateExpiration(maintenant.plusDays(Reservation.DUREE_VALIDITE_JOURS));

        reservation.setStatut(StatutReservation.EN_ATTENTE);

        Reservation enregistree = reservationRepository.save(reservation);
        log.info("Reservation {} creee : livre {} pour l'adherent {}, echeance {}",
                enregistree.getReservationId(), livre.getBookId(),
                adherentActif.getUserId(), enregistree.getDateExpiration());

        return versDto(enregistree);
    }

    // ------------------------------------------------------------------
    //  Consultation (RS-05 : filtrage par role)
    // ------------------------------------------------------------------

    /**
     * Liste les reservations.
     *
     * RS-05 : un ADHERENT ne voit que ses propres reservations.
     * Un BIBLIOTHECAIRE voit toutes les reservations.
     *
     * @param statut           filtre optionnel sur le statut
     * @param adherentId       filtre optionnel sur l'adherent (pour admin)
     * @param adherentCourant  l'utilisateur authentifie
     * @param estBibliothecaire true si le role est BIBLIOTHECAIRE
     */
    @Transactional(readOnly = true)
    public List<ReservationResponseDto> lister(StatutReservation statut, Integer adherentId,
                                               Users adherentCourant, boolean estBibliothecaire) {

        // RS-05 : un ADHERENT ne voit que ses propres reservations.
        if (!estBibliothecaire) {
            adherentId = adherentCourant.getUserId();
        }

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

    /**
     * RS-03 : un ADHERENT ne peut consulter qu'une reservation qui lui appartient.
     * Un BIBLIOTHECAIRE peut consulter n'importe quelle reservation.
     */
    @Transactional(readOnly = true)
    public ReservationResponseDto consulter(Integer id, Users adherentCourant, boolean estBibliothecaire) {
        Reservation reservation = chercher(id);

        // RS-03 : verification du proprietaire
        if (!estBibliothecaire
                && !reservation.getAdherent().getUserId().equals(adherentCourant.getUserId())) {
            log.warn("Acces refuse : l'adherent {} tente d'acceder a la reservation {} qui appartient a {}",
                    adherentCourant.getUserId(), id, reservation.getAdherent().getUserId());
            throw new AccessDeniedException(
                    "Vous n'etes pas autorise a consulter cette reservation.");
        }

        return versDto(reservation);
    }

    /** Bonus : les reservations dont l'echeance a ete constatee depassee. */
    @Transactional(readOnly = true)
    public List<ReservationResponseDto> listerExpirees() {
        return versDtos(reservationRepository
                .findByStatutOrderByDateReservationDesc(StatutReservation.EXPIREE));
    }

    // ------------------------------------------------------------------
    //  Transitions d'etat (RS-03 : verification du proprietaire)
    // ------------------------------------------------------------------

    /**
     * Annule une reservation.
     *
     * RS-03 : un ADHERENT ne peut annuler qu'une reservation qui lui appartient.
     *
     * RG-05 n'autorise l'annulation que depuis EN_ATTENTE ou DISPONIBLE, et
     * RG-06 gele les trois autres statuts.
     */
    public ReservationResponseDto annuler(Integer id, Users adherentCourant, boolean estBibliothecaire) {

        Reservation reservation = chercher(id);

        // RS-03 : verification du proprietaire
        if (!estBibliothecaire
                && !reservation.getAdherent().getUserId().equals(adherentCourant.getUserId())) {
            log.warn("Acces refuse : l'adherent {} tente d'annuler la reservation {} qui appartient a {}",
                    adherentCourant.getUserId(), id, reservation.getAdherent().getUserId());
            throw new AccessDeniedException(
                    "Vous n'etes pas autorise a annuler cette reservation.");
        }

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
