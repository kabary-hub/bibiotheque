# Séance 2 — Module Réservation

> **Branche :** `feature/reservation-boubacar-siddighi-balde` → `main`

---

## Ce que fait cette PR

Un adhérent peut réserver un livre **indisponible** et sera prévenu quand un exemplaire revient.
Le module suit le découpage du projet existant — entité, repository, service, contrôleur — avec
un DTO en entrée et un DTO en sortie. **Aucune logique métier dans le contrôleur**, et l'entité
`Reservation` ne sort jamais du service : `ReservationService.versDto()` est son unique porte de
sortie.

## Les cinq endpoints

| Verbe | Chemin | Succès | Erreurs |
|---|---|---|---|
| `POST` | `/api/reservations` | 201 | 400, 404, 409 |
| `GET` | `/api/reservations` | 200 | 400 *(statut inconnu)* |
| `GET` | `/api/reservations/{id}` | 200 | 404 |
| `PATCH` | `/api/reservations/{id}/annuler` | 200 | 404, 409 |
| `DELETE` | `/api/reservations/{id}` | 204 | 404 |

Le client n'envoie que `livreId` et `adherentId`. La date de réservation, la date d'expiration
et le statut initial sont déterminés par le serveur.

**Bonus :** `GET /api/reservations/expirees`, et bascule automatique en `EXPIREE`.

## Où chaque règle est implémentée

| Réf. | Une phrase | Emplacement |
|---|---|---|
| **RG-01** | Le service refuse la création si `noOfCopies > 0` : un livre encore en rayon s'emprunte, il ne se réserve pas. | `service/ReservationService.java` — test L73, prédicat `estDisponible()` L236 |
| **RG-02** | Avant de créer, le service demande au repository s'il existe déjà une réservation **active** de cet adhérent sur ce livre. | `service/ReservationService.java` L81-82, requête `existsByAdherentUserIdAndLivreBookIdAndStatutIn` |
| **RG-03** | Le service compte les réservations actives de l'adhérent et refuse la quatrième. | `service/ReservationService.java` L90-96, constante `MAX_RESERVATIONS_ACTIVES = 3` L39 |
| **RG-04** | `dateExpiration = dateReservation + 7 jours`, à partir d'un **unique** appel à `LocalDateTime.now()` côté serveur. | `service/ReservationService.java` L107-109, constante `Reservation.DUREE_VALIDITE_JOURS` |
| **RG-05** | L'annulation n'est acceptée que si le statut courant est actif, c'est-à-dire `EN_ATTENTE` ou `DISPONIBLE`. | `service/ReservationService.java` — `annuler()` L173, garde L177 |
| **RG-06** | Les trois statuts terminaux sont déclarés dans l'énumération et consultés partout où un état change. | `entity/StatutReservation.java` — `estActif()` / `estTerminal()` |

## Gestion des erreurs

`ReservationExceptionHandler` traduit les exceptions en réponses HTTP au format unique `ApiError` :

```json
{
  "timestamp": "2026-08-31T19:14:02",
  "status": 409,
  "error": "Conflict",
  "message": "RG-03 : l'adherent a2_quota a deja 3 reservation(s) active(s)...",
  "regle": "RG-03"
}
```

## Tests — 26, tous verts

| Classe | Nb | Portée |
|---|---|---|
| `ReservationServiceTest` | 10 | Chaque règle RG-01 à RG-06, avec Mockito |
| `ReservationControllerTest` | 15 | Les cinq endpoints de bout en bout sur H2 |
| `BibliothequeApplicationTests` | 1 | Chargement du contexte |

## Vérification

```bash
cd bibliotheque-backend
./mvnw.cmd test
```

Puis `docker compose up -d` et ouvrir http://localhost:8080/swagger-ui.html.
