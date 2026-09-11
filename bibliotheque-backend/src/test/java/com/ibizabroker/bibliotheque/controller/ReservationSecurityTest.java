package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.StatutReservation;
import com.ibizabroker.bibliotheque.entity.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de securite du module Reservation (RS-01 a RS-05).
 *
 * Ces tests verifient que :
 * - RS-01 : sans token, tout endpoint renvoie 401
 * - RS-02 : un ADHERENT qui tente un DELETE recoit 403
 * - RS-03 : un ADHERENT ne peut consulter qu'une reservation qui lui appartient
 * - RS-04 : l'identite vient du token, pas du corps (teste via creation)
 * - RS-05 : un ADHERENT ne voit que ses propres reservations
 *
 * Base H2 en memoire, aucune BDD externe requise.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Securite du module Reservation")
class ReservationSecurityTest {

    private static final String BASE = "/api/reservations";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private BooksRepository booksRepository;

    @Autowired
    private UsersRepository usersRepository;

    private Books livreIndisponible;
    private Users adherent1;
    private Users adherent2;
    private Users bibliothecaire;

    @BeforeEach
    void preparer() {
        reservationRepository.deleteAll();
        booksRepository.deleteAll();
        usersRepository.deleteAll();

        livreIndisponible = new Books();
        livreIndisponible.setBookName("Livre de test");
        livreIndisponible.setBookAuthor("Auteur");
        livreIndisponible.setBookGenre("Roman");
        livreIndisponible.setNoOfCopies(0);
        booksRepository.save(livreIndisponible);

        adherent1 = new Users();
        adherent1.setUsername("adherent1");
        adherent1.setName("Adherent Alpha");
        adherent1.setPassword("motdepasse");
        usersRepository.save(adherent1);

        adherent2 = new Users();
        adherent2.setUsername("adherent2");
        adherent2.setName("Adherent Beta");
        adherent2.setPassword("motdepasse");
        usersRepository.save(adherent2);

        bibliothecaire = new Users();
        bibliothecaire.setUsername("biblio");
        bibliothecaire.setName("Bibliothecaire");
        bibliothecaire.setPassword("motdepasse");
        usersRepository.save(bibliothecaire);
    }

    // ==================================================================
    //  RS-01 : sans token, tout endpoint renvoie 401
    // ==================================================================

    @Test
    @DisplayName("RS-01 : GET /api/reservations sans token -> 401")
    void getReservationsSansTokenRetourne401() throws Exception {
        mockMvc.perform(get(BASE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RS-01 : POST /api/reservations sans token -> 401")
    void postReservationSansTokenRetourne401() throws Exception {
        Map<String, Integer> corps = new HashMap<>();
        corps.put("livreId", livreIndisponible.getBookId());

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper().writeValueAsString(corps)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RS-01 : GET /api/reservations/{id} sans token -> 401")
    void getReservationParIdSansTokenRetourne401() throws Exception {
        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RS-01 : PATCH /api/reservations/{id}/annuler sans token -> 401")
    void patchAnnulerSansTokenRetourne401() throws Exception {
        mockMvc.perform(patch(BASE + "/1/annuler"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RS-01 : DELETE /api/reservations/{id} sans token -> 401")
    void deleteReservationSansTokenRetourne401() throws Exception {
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isUnauthorized());
    }

    // ==================================================================
    //  RS-02 : un ADHERENT qui tente un DELETE recoit 403
    // ==================================================================

    @Test
    @DisplayName("RS-02 : ADHERENT tente DELETE -> 403")
    void adherentTenteDeleteRetourne403() throws Exception {
        Reservation reservation = creerReservation(livreIndisponible, adherent1);

        mockMvc.perform(
                        org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("adherent1").roles("ADHERENT")
                                .apply(new org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor() {
                                    @Override
                                    public org.springframework.security.core.userdetails.UserDetails postProcess(org.springframework.security.core.userdetails.UserDetails user) {
                                        return user;
                                    }
                                }))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("RS-02 : ADHERENT tente DELETE -> 403")
    void adherentTenteDeleteRetourne403v2() throws Exception {
        Reservation reservation = creerReservation(livreIndisponible, adherent1);

        mockMvc.perform(delete(BASE + "/" + reservation.getReservationId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("adherent1").roles("ADHERENT")))
                .andExpect(status().isForbidden());
    }

    // ==================================================================
    //  RS-03 : un ADHERENT ne peut consulter qu'une reservation qui lui appartient
    // ==================================================================

    @Test
    @DisplayName("RS-03 : ADHERENT consulte sa propre reservation -> 200")
    void adherentConsulteSaPropreReservationRetourne200() throws Exception {
        Reservation reservation = creerReservation(livreIndisponible, adherent1);

        mockMvc.perform(get(BASE + "/" + reservation.getReservationId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("adherent1").roles("ADHERENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservation.getReservationId()));
    }

    @Test
    @DisplayName("RS-03 : ADHERENT consulte la reservation d'un autre -> 403")
    void adherentConsulteReservationDunAutreRetourne403() throws Exception {
        Reservation reservation = creerReservation(livreIndisponible, adherent2);

        mockMvc.perform(get(BASE + "/" + reservation.getReservationId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("adherent1").roles("ADHERENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RS-03 : ADHERENT annule la reservation d'un autre -> 403")
    void adherentAnnuleReservationDunAutreRetourne403() throws Exception {
        Reservation reservation = creerReservation(livreIndisponible, adherent2);

        mockMvc.perform(patch(BASE + "/" + reservation.getReservationId() + "/annuler")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("adherent1").roles("ADHERENT")))
                .andExpect(status().isForbidden());
    }

    // ==================================================================
    //  RS-04 : l'identite vient du token, pas du corps
    // ==================================================================

    @Test
    @DisplayName("RS-04 : la reservation est creee au nom de l'utilisateur du token, pas d'un autre")
    void reservationCreeeAuNomDuToken() throws Exception {
        Map<String, Integer> corps = new HashMap<>();
        corps.put("livreId", livreIndisponible.getBookId());

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper().writeValueAsString(corps))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("adherent1").roles("ADHERENT")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adherentId").value(adherent1.getUserId()));
    }

    // ==================================================================
    //  RS-05 : un ADHERENT ne voit que ses propres reservations
    // ==================================================================

    @Test
    @DisplayName("RS-05 : ADHERENT voit uniquement ses propres reservations")
    void adherentNeVoitQueSesPropresReservations() throws Exception {
        creerReservation(livreIndisponible, adherent1);

        Books autreLivre = new Books();
        autreLivre.setBookName("Autre livre");
        autreLivre.setBookAuthor("Auteur 2");
        autreLivre.setBookGenre("Roman");
        autreLivre.setNoOfCopies(0);
        booksRepository.save(autreLivre);

        creerReservation(autreLivre, adherent2);

        mockMvc.perform(get(BASE)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("adherent1").roles("ADHERENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].adherentId").value(adherent1.getUserId()));
    }

    @Test
    @DisplayName("RS-05 : BIBLIOTHECAIRE voit toutes les reservations")
    void bibliothecaireVoitToutesLesReservations() throws Exception {
        creerReservation(livreIndisponible, adherent1);

        Books autreLivre = new Books();
        autreLivre.setBookName("Autre livre");
        autreLivre.setBookAuthor("Auteur 2");
        autreLivre.setBookGenre("Roman");
        autreLivre.setNoOfCopies(0);
        booksRepository.save(autreLivre);

        creerReservation(autreLivre, adherent2);

        mockMvc.perform(get(BASE)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("biblio").roles("BIBLIOTHECAIRE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ==================================================================
    //  BIBLIOTHECAIRE : peut tout faire
    // ==================================================================

    @Test
    @DisplayName("BIBLIOTHECAIRE peut consulter la reservation d'un adherent -> 200")
    void bibliothecairePeutConsulterReservationDunAdherent() throws Exception {
        Reservation reservation = creerReservation(livreIndisponible, adherent1);

        mockMvc.perform(get(BASE + "/" + reservation.getReservationId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("biblio").roles("BIBLIOTHECAIRE")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("BIBLIOTHECAIRE peut annuler la reservation d'un adherent -> 200")
    void bibliothecairePeutAnnulerReservationDunAdherent() throws Exception {
        Reservation reservation = creerReservation(livreIndisponible, adherent1);

        mockMvc.perform(patch(BASE + "/" + reservation.getReservationId() + "/annuler")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("biblio").roles("BIBLIOTHECAIRE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ANNULEE"));
    }

    @Test
    @DisplayName("BIBLIOTHECAIRE peut supprimer une reservation -> 204")
    void bibliothecairePeutSupprimerReservation() throws Exception {
        Reservation reservation = creerReservation(livreIndisponible, adherent1);

        mockMvc.perform(delete(BASE + "/" + reservation.getReservationId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("biblio").roles("BIBLIOTHECAIRE")))
                .andExpect(status().isNoContent());
    }

    // ==================================================================
    //  Helpers
    // ==================================================================

    private Reservation creerReservation(Books livre, Users adherent) {
        Reservation reservation = new Reservation();
        reservation.setLivre(livre);
        reservation.setAdherent(adherent);
        reservation.setDateReservation(LocalDateTime.now());
        reservation.setDateExpiration(LocalDateTime.now().plusDays(Reservation.DUREE_VALIDITE_JOURS));
        reservation.setStatut(StatutReservation.EN_ATTENTE);
        return reservationRepository.save(reservation);
    }

    private com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper();
    }
}
