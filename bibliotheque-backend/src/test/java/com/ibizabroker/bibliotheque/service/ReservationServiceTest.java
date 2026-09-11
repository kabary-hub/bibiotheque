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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regles de gestion du module Reservation, testees sans base ni serveur.
 *
 * Les repositories sont simules : chaque test decrit un etat du systeme et
 * verifie la decision prise. C'est precisement ce que permet d'avoir sorti la
 * logique du controleur.
 *
 * Depuis la seance 4, creer() recoit directement l'entite Users (RS-04) :
 * le test passe donc explicitement l'adherent au lieu de le faire croire
 * venu du DTO.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService - regles de gestion")
class ReservationServiceTest {

    private static final Integer ID_LIVRE = 1;
    private static final Integer ID_ADHERENT = 2;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BooksRepository booksRepository;

    @Mock
    private UsersRepository usersRepository;

    @InjectMocks
    private ReservationService reservationService;

    private Books livreIndisponible;
    private Users adherent;
    private ReservationRequestDto demande;

    @BeforeEach
    void preparer() {
        livreIndisponible = new Books();
        livreIndisponible.setBookId(ID_LIVRE);
        livreIndisponible.setBookName("Le Petit Prince");
        livreIndisponible.setNoOfCopies(0);

        adherent = new Users();
        adherent.setUserId(ID_ADHERENT);
        adherent.setUsername("lecteur");
        adherent.setName("Jean Dupont");

        demande = new ReservationRequestDto();
        demande.setLivreId(ID_LIVRE);
        // Plus de demande.setAdherentId() : RS-04 l'interdit.
        // L'adherent est passe directement a creer().
    }

    // ------------------------------------------------------------------
    //  RG-03 : trois reservations actives au maximum
    // ------------------------------------------------------------------

    @Test
    @DisplayName("RG-03 : une quatrieme reservation active est refusee")
    void refuseCreationQuandAdherentATroisReservationsActives() {

        when(booksRepository.findById(ID_LIVRE)).thenReturn(Optional.of(livreIndisponible));
        when(usersRepository.findById(ID_ADHERENT)).thenReturn(Optional.of(adherent));
        when(reservationRepository.existsByAdherentUserIdAndLivreBookIdAndStatutIn(
                eq(ID_ADHERENT), eq(ID_LIVRE), anyCollection())).thenReturn(false);
        when(reservationRepository.countByAdherentUserIdAndStatutIn(
                eq(ID_ADHERENT), anyCollection())).thenReturn(3L);

        assertThatThrownBy(() -> reservationService.creer(demande, adherent))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("RG-03")
                .hasMessageContaining("maximum autorise est de 3");

        // Rien ne doit avoir ete ecrit : un refus ne laisse pas de trace.
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("RG-03 : la troisieme reservation active passe encore")
    void peutCreerUneReservationQuandAdherentADeuxReservationsActives() {

        when(booksRepository.findById(ID_LIVRE)).thenReturn(Optional.of(livreIndisponible));
        when(usersRepository.findById(ID_ADHERENT)).thenReturn(Optional.of(adherent));
        when(reservationRepository.existsByAdherentUserIdAndLivreBookIdAndStatutIn(
                eq(ID_ADHERENT), eq(ID_LIVRE), anyCollection())).thenReturn(false);
        when(reservationRepository.countByAdherentUserIdAndStatutIn(
                eq(ID_ADHERENT), anyCollection())).thenReturn(2L);
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponseDto resultat = reservationService.creer(demande, adherent);

        assertThat(resultat.getStatut()).isEqualTo(StatutReservation.EN_ATTENTE);
        assertThat(resultat.getLivreId()).isEqualTo(ID_LIVRE);
        assertThat(resultat.getAdherentId()).isEqualTo(ID_ADHERENT);
    }

    @Test
    @DisplayName("RG-03 : le comptage ne porte que sur EN_ATTENTE et DISPONIBLE")
    void neCompteQueLesStatutsActifs() {

        when(booksRepository.findById(ID_LIVRE)).thenReturn(Optional.of(livreIndisponible));
        when(usersRepository.findById(ID_ADHERENT)).thenReturn(Optional.of(adherent));
        when(reservationRepository.existsByAdherentUserIdAndLivreBookIdAndStatutIn(
                eq(ID_ADHERENT), eq(ID_LIVRE), anyCollection())).thenReturn(false);
        when(reservationRepository.countByAdherentUserIdAndStatutIn(
                eq(ID_ADHERENT), anyCollection())).thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        reservationService.creer(demande, adherent);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<StatutReservation>> captor =
                ArgumentCaptor.forClass(Collection.class);
        verify(reservationRepository).countByAdherentUserIdAndStatutIn(eq(ID_ADHERENT), captor.capture());

        assertThat(captor.getValue())
                .containsExactlyInAnyOrder(StatutReservation.EN_ATTENTE, StatutReservation.DISPONIBLE);
    }

    // ------------------------------------------------------------------
    //  RG-01, RG-02, RG-04
    // ------------------------------------------------------------------

    @Test
    @DisplayName("RG-01 : reserver un livre disponible est refuse")
    void refuseUnLivreDisponible() {

        Books livreDisponible = new Books();
        livreDisponible.setBookId(ID_LIVRE);
        livreDisponible.setBookName("Le Petit Prince");
        livreDisponible.setNoOfCopies(2);

        when(booksRepository.findById(ID_LIVRE)).thenReturn(Optional.of(livreDisponible));
        when(usersRepository.findById(ID_ADHERENT)).thenReturn(Optional.of(adherent));

        assertThatThrownBy(() -> reservationService.creer(demande, adherent))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("RG-01");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("RG-02 : deux reservations actives sur le meme livre sont refusees")
    void refuseUnDoublonActif() {

        when(booksRepository.findById(ID_LIVRE)).thenReturn(Optional.of(livreIndisponible));
        when(usersRepository.findById(ID_ADHERENT)).thenReturn(Optional.of(adherent));
        when(reservationRepository.existsByAdherentUserIdAndLivreBookIdAndStatutIn(
                eq(ID_ADHERENT), eq(ID_LIVRE), anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> reservationService.creer(demande, adherent))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("RG-02");

        verify(reservationRepository, never()).countByAdherentUserIdAndStatutIn(anyInt(), anyCollection());
    }

    @Test
    @DisplayName("RG-04 : l'expiration tombe exactement sept jours apres la reservation")
    void calculeLexpirationASeptJours() {

        when(booksRepository.findById(ID_LIVRE)).thenReturn(Optional.of(livreIndisponible));
        when(usersRepository.findById(ID_ADHERENT)).thenReturn(Optional.of(adherent));
        when(reservationRepository.existsByAdherentUserIdAndLivreBookIdAndStatutIn(
                eq(ID_ADHERENT), eq(ID_LIVRE), anyCollection())).thenReturn(false);
        when(reservationRepository.countByAdherentUserIdAndStatutIn(
                eq(ID_ADHERENT), anyCollection())).thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime avant = LocalDateTime.now();
        ReservationResponseDto resultat = reservationService.creer(demande, adherent);

        assertThat(resultat.getDateReservation()).isAfterOrEqualTo(avant);
        assertThat(Duration.between(resultat.getDateReservation(), resultat.getDateExpiration()))
                .isEqualTo(Duration.ofDays(Reservation.DUREE_VALIDITE_JOURS));
    }

    @Test
    @DisplayName("404 : un livre inconnu n'est pas un conflit metier")
    void livreInconnuLeveNotFound() {

        when(booksRepository.findById(ID_LIVRE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.creer(demande, adherent))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Le livre d'identifiant 1 n'existe pas.");
    }

    // ------------------------------------------------------------------
    //  RG-05 et RG-06
    // ------------------------------------------------------------------

    @Test
    @DisplayName("RG-05 : une reservation EN_ATTENTE peut etre annulee")
    void annuleUneReservationActive() {

        Reservation enAttente = reservation(StatutReservation.EN_ATTENTE);
        when(reservationRepository.findById(10)).thenReturn(Optional.of(enAttente));
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponseDto resultat = reservationService.annuler(10, adherent, true);

        assertThat(resultat.getStatut()).isEqualTo(StatutReservation.ANNULEE);
    }

    @Test
    @DisplayName("RG-06 : une reservation deja ANNULEE ne peut plus changer d'etat")
    void refuseDeReannulerUnStatutTerminal() {

        Reservation annulee = reservation(StatutReservation.ANNULEE);
        when(reservationRepository.findById(10)).thenReturn(Optional.of(annulee));

        assertThatThrownBy(() -> reservationService.annuler(10, adherent, true))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("RG-05")
                .hasMessageContaining("RG-06");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("RG-06 : le passage automatique en EXPIREE ignore les statuts terminaux")
    void expirationNeCibleQueLesReservationsActives() {

        when(reservationRepository.findByStatutInAndDateExpirationBefore(anyCollection(), any()))
                .thenReturn(java.util.Collections.singletonList(reservation(StatutReservation.EN_ATTENTE)));

        int basculees = reservationService.expirerReservationsEchues();

        assertThat(basculees).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<StatutReservation>> captor =
                ArgumentCaptor.forClass(Collection.class);
        verify(reservationRepository).findByStatutInAndDateExpirationBefore(captor.capture(), any());

        assertThat(captor.getValue())
                .containsExactlyInAnyOrder(StatutReservation.EN_ATTENTE, StatutReservation.DISPONIBLE)
                .doesNotContain(StatutReservation.ANNULEE, StatutReservation.HONOREE);
    }

    // ------------------------------------------------------------------
    //  RS-03 : verification du proprietaire (service layer)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("RS-03 : un ADHERENT ne peut consulter que sa propre reservation")
    void refuseConsulterReservationDunAutre() {
        Reservation reservationDunAutre = reservation(StatutReservation.EN_ATTENTE);
        // La reservation appartient a l'adherent (ID=2), mais on appelle avec un autre adherent
        Users autreAdherent = new Users();
        autreAdherent.setUserId(99);
        autreAdherent.setUsername("autre");

        when(reservationRepository.findById(10)).thenReturn(Optional.of(reservationDunAutre));

        assertThatThrownBy(() -> reservationService.consulter(10, autreAdherent, false))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("autorise");
    }

    @Test
    @DisplayName("RS-03 : un BIBLIOTHECAIRE peut consulter la reservation de n'importe qui")
    void bibliothecairePeutConsulterTout() {
        Reservation reservation = reservation(StatutReservation.EN_ATTENTE);
        when(reservationRepository.findById(10)).thenReturn(Optional.of(reservation));

        ReservationResponseDto resultat = reservationService.consulter(10, adherent, true);
        assertThat(resultat.getId()).isEqualTo(10);
    }

    @Test
    @DisplayName("RS-03 : un ADHERENT ne peut annuler que sa propre reservation")
    void refuseAnnulerReservationDunAutre() {
        Reservation reservationDunAutre = reservation(StatutReservation.EN_ATTENTE);
        Users autreAdherent = new Users();
        autreAdherent.setUserId(99);
        autreAdherent.setUsername("autre");

        when(reservationRepository.findById(10)).thenReturn(Optional.of(reservationDunAutre));

        assertThatThrownBy(() -> reservationService.annuler(10, autreAdherent, false))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------

    private Reservation reservation(StatutReservation statut) {
        Reservation reservation = new Reservation();
        reservation.setReservationId(10);
        reservation.setLivre(livreIndisponible);
        reservation.setAdherent(adherent);
        reservation.setDateReservation(LocalDateTime.now().minusDays(1));
        reservation.setDateExpiration(LocalDateTime.now().plusDays(6));
        reservation.setStatut(statut);
        return reservation;
    }
}
