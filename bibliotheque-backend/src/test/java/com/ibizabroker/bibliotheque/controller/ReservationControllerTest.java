package com.ibizabroker.bibliotheque.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Les cinq endpoints traverses de bout en bout : validation, securite, service,
 * JPA et serialisation, sur une base H2 en memoire.
 *
 * Chaque test est annot\u00e9 avec @WithMockUser pour simuler un utilisateur
 * authentifi\u00e9 (obligatoire depuis la securisation RS-01).
 *
 * Le test unitaire ReservationServiceTest verifie les decisions metier ; celui-ci
 * verifie que chaque decision ressort avec le bon code HTTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API /api/reservations")
class ReservationControllerTest {

    private static final String BASE = "/api/reservations";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private BooksRepository booksRepository;

    @Autowired
    private UsersRepository usersRepository;

    private Books livreIndisponible;
    private Books livreDisponible;
    private Users adherent;

    @BeforeEach
    void preparerLeJeuDeDonnees() {
        // Les reservations d'abord : elles referencent les deux autres tables.
        reservationRepository.deleteAll();
        booksRepository.deleteAll();
        usersRepository.deleteAll();

        livreIndisponible = livre("Le Petit Prince", 0);
        livreDisponible = livre("Germinal", 3);
        adherent = adherent("lecteur", "Jean Dupont");
    }

    // ------------------------------------------------------------------
    //  POST /api/reservations
    // ------------------------------------------------------------------

    @Test
    @WithMockUser(username = "lecteur", roles = {"ADHERENT"})
    @DisplayName("POST sans livreId : 400, et le message nomme le champ manquant")
    void postSansLivreId() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("livreId")));
    }

    @Test
    @WithMockUser(username = "lecteur", roles = {"ADHERENT"})
    @DisplayName("POST sur un livre inconnu : 404")
    void postLivreInconnu() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(999_999)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("999999")));
    }

    @Test
    @WithMockUser(username = "lecteur", roles = {"ADHERENT"})
    @DisplayName("RG-01 : POST sur un livre disponible : 409")
    void postLivreDisponible() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(livreDisponible.getBookId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.regle").value("RG-01"));
    }

    @Test
    @WithMockUser(username = "lecteur", roles = {"ADHERENT"})
    @DisplayName("POST sur un livre indisponible : 201, statut EN_ATTENTE, echeance a 7 jours")
    void postLivreIndisponible() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(livreIndisponible.getBookId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.livreId").value(livreIndisponible.getBookId()))
                .andExpect(jsonPath("$.adherentId").value(adherent.getUserId()))
                .andExpect(jsonPath("$.dateReservation").exists())
                .andExpect(jsonPath("$.dateExpiration").exists());

        Reservation creee = reservationRepository.findAll().get(0);
        // RG-04, verifiee sur la ligne reellement persistee.
        org.assertj.core.api.Assertions.assertThat(creee.getDateExpiration())
                .isEqualTo(creee.getDateReservation().plusDays(Reservation.DUREE_VALIDITE_JOURS));
    }

    @Test
    @WithMockUser(username = "lecteur", roles = {"ADHERENT"})
    @DisplayName("RG-02 : deux POST sur le meme livre par le meme adherent : 409")
    void postDoublon() throws Exception {
        String demande = corps(livreIndisponible.getBookId());

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(demande))
                .andExpect(status().isCreated());

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(demande))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.regle").value("RG-02"));
    }

    @Test
    @WithMockUser(username = "lecteur", roles = {"ADHERENT"})
    @DisplayName("RG-03 : la quatrieme reservation active : 409")
    void postQuatriemeReservation() throws Exception {
        for (int i = 1; i <= 3; i++) {
            Books autre = livre("Titre indisponible " + i, 0);
            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corps(autre.getBookId())))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(livreIndisponible.getBookId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.regle").value("RG-03"));
    }

    // ------------------------------------------------------------------
    //  GET
    // ------------------------------------------------------------------

    @Test
    @WithMockUser(username = "lecteur", roles = {"ADHERENT"})
    @DisplayName("GET liste : filtres par statut et par adherent")
    void getListeFiltrable() throws Exception {
        creerEnBase(StatutReservation.EN_ATTENTE);
        creerEnBase(StatutReservation.ANNULEE);

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get(BASE).param("statut", "EN_ATTENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].statut").value("EN_ATTENTE"));

        mockMvc.perform(get(BASE).param("adherentId", String.valueOf(adherent.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get(BASE)
                        .param("adherentId", String.valueOf(adherent.getUserId()))
                        .param("statut", "ANNULEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @WithMockUser(username = "lecteur", roles = {"ADHERENT"})
    @DisplayName("GET liste : un statut inconnu est refuse en 400, pas en 500")
    void getStatutInvalide() throws Exception {
        mockMvc.perform(get(BASE).param("statut", "PEUT_ETRE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("PEUT_ETRE")));
    }

    @Test
    @WithMockUser(username = "lecteur", roles = {"ADHERENT"})
    @DisplayName("GET /{id} : 200 si connue, 404 sinon")
    void getParIdentifiant() throws Exception {
        Reservation reservation = creerEnBase(StatutReservation.EN_ATTENTE);

        mockMvc.perform(get(BASE + "/" + reservation.getReservationId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservation.getReservationId()));

        mockMvc.perform(get(BASE + "/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "lecteur", roles = {"ADHERENT"})
    @DisplayName("GET /expirees : ne renvoie que les reservations EXPIREE")
    void getExpirees() throws Exception {
        creerEnBase(StatutReservation.EN_ATTENTE);
        creerEnBase(StatutReservation.EXPIREE);

        mockMvc.perform(get(BASE + "/expirees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].statut").value("EXPIREE"));
    }

    // ------------------------------------------------------------------
    //  PATCH et DELETE
    // ------------------------------------------------------------------

    @Test
    @WithMockUser(username = "lecteur", roles = {"ADHERENT"})
    @DisplayName("RG-05 : PATCH annuler passe une fois, puis 409")
    void patchAnnuler() throws Exception {
        Reservation reservation = creerEnBase(StatutReservation.EN_ATTENTE);
        String chemin = BASE + "/" + reservation.getReservationId() + "/annuler";

        mockMvc.perform(patch(chemin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ANNULEE"));

        // RG-06 : le statut terminal est definitif.
        mockMvc.perform(patch(chemin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.regle").value("RG-05"));
    }

    @Test
    @WithMockUser(username = "lecteur", roles = {"ADHERENT"})
    @DisplayName("PATCH annuler sur une reservation inconnue : 404")
    void patchAnnulerInconnue() throws Exception {
        mockMvc.perform(patch(BASE + "/999999/annuler"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "biblio", roles = {"BIBLIOTHECAIRE"})
    @DisplayName("DELETE : 204 puis 404")
    void deleteReservation() throws Exception {
        Reservation reservation = creerEnBase(StatutReservation.EN_ATTENTE);
        String chemin = BASE + "/" + reservation.getReservationId();

        mockMvc.perform(delete(chemin)).andExpect(status().isNoContent());
        mockMvc.perform(delete(chemin)).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    //  Documentation
    // ------------------------------------------------------------------

    @Test
    @WithMockUser(username = "lecteur", roles = {"ADHERENT"})
    @DisplayName("Swagger expose les cinq endpoints avec leurs codes de retour")
    void contratOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/reservations'].post.responses.201").exists())
                .andExpect(jsonPath("$.paths['/api/reservations'].post.responses.400").exists())
                .andExpect(jsonPath("$.paths['/api/reservations'].post.responses.404").exists())
                .andExpect(jsonPath("$.paths['/api/reservations'].post.responses.409").exists())
                .andExpect(jsonPath("$.paths['/api/reservations'].get.responses.200").exists())
                .andExpect(jsonPath("$.paths['/api/reservations/{id}'].get.responses.200").exists())
                .andExpect(jsonPath("$.paths['/api/reservations/{id}'].get.responses.404").exists())
                .andExpect(jsonPath("$.paths['/api/reservations/{id}'].delete.responses.204").exists())
                .andExpect(jsonPath("$.paths['/api/reservations/{id}'].delete.responses.404").exists())
                .andExpect(jsonPath("$.paths['/api/reservations/{id}/annuler'].patch.responses.200").exists())
                .andExpect(jsonPath("$.paths['/api/reservations/{id}/annuler'].patch.responses.409").exists());
    }

    // ------------------------------------------------------------------
    //  Fabriques
    // ------------------------------------------------------------------

    private String corps(Integer livreId) throws Exception {
        Map<String, Integer> demande = new HashMap<>();
        demande.put("livreId", livreId);
        return objectMapper.writeValueAsString(demande);
    }

    private Books livre(String titre, int exemplaires) {
        Books livre = new Books();
        livre.setBookName(titre);
        livre.setBookAuthor("Auteur");
        livre.setBookGenre("Roman");
        livre.setNoOfCopies(exemplaires);
        return booksRepository.save(livre);
    }

    private Users adherent(String identifiant, String nom) {
        Users adherent = new Users();
        adherent.setUsername(identifiant);
        adherent.setName(nom);
        adherent.setPassword("motdepasse");
        return usersRepository.save(adherent);
    }

    private Reservation creerEnBase(StatutReservation statut) {
        Reservation reservation = new Reservation();
        reservation.setLivre(livreIndisponible);
        reservation.setAdherent(adherent);
        reservation.setDateReservation(LocalDateTime.now());
        reservation.setDateExpiration(LocalDateTime.now().plusDays(Reservation.DUREE_VALIDITE_JOURS));
        reservation.setStatut(statut);
        return reservationRepository.save(reservation);
    }
}
