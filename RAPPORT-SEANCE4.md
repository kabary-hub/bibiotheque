# Rapport Seance 4 — Securisation et tests du module Reservation

## 1. Resume executif

La seance 4 avait pour objectif de securiser l'API de reservation de bibliotheque, qui etait auparavant ouverte a tous (permitAll). Cinq regles de securite (RS-01 a RS-05) ont ete implementees : authentification obligatoire (RS-01), restriction du DELETE aux BIBLIOTHECAIRE (RS-02), verification du proprietaire (RS-03), recuperation de l'identite depuis le token JWT (RS-04), et filtrage par role pour le listage (RS-05). Les tests unitaires RG-03 et les tests d'integration de securite ont ete crees et passes en vert. La collection Postman a ete mise a jour avec des scripts de verification automatiques.

---

## 2. Audit initial

### Stack technique
- Spring Boot 2.4.5, Java 1.8, Maven
- PostgreSQL 16 (Docker), H2 pour les tests
- JWT (jjwt 0.9.1), Spring Security deja actif
- SpringDoc OpenAPI 1.5.13

### Securite existante
- `WebSecurityConfigurerAdapter` avec filtre JWT
- `JwtAuthenticationEntryPoint` → 401 si token absent/invalide
- `ApiExceptionHandler` → 403 si `AccessDeniedException`
- `/api/reservations/**` etait en `permitAll()` : **aucune securite**

### Roles existants
- `Admin` (gestion livres/utilisateurs)
- `User` (emprunts)
- **Aucun role ADHERENT ni BIBLIOTHECAIRE** → a ajouter

### Endpoints existants
- POST/GET/PATCH/DELETE `/api/reservations` — tous en permitAll
- `ReservationRequestDto` contenait `adherentId` envoye par le client (faille RS-04)

### Tests existants
- `ReservationServiceTest` : RG-01 a RG-06 avec mocks
- `ReservationControllerTest` : validation, codes HTTP, OpenAPI
- **Aucun test de securite**

### Collection Postman
- `epreuve/seance-2/bibliotheque-reservation.postman_collection.json`
- Contenait des tests basiques de securite (6.4 : GET sans token → 200 car permitAll)

---

## 3. Flux detailles

### a) Flux d'authentification
```
Client → POST /authenticate (username, password)
       → JwtController.createJwtToken()
       → JwtService.createJwtToken()
          → AuthenticationManager.authenticate()
          → JwtService.loadUserByUsername()
             → UsersRepository.findByUsername()
             → Construction des autorites : ROLE_ADHERENT, ROLE_BIBLIOTHECAIRE
          → JwtUtil.generateToken(userDetails)
       → Retourne JwtResponse(user, jwtToken)
```

### b) Flux d'une requete authentifiee
```
Client → GET /api/reservations (Header: Authorization: Bearer xxx)
       → JwtRequestFilter.doFilterInternal()
          → JwtUtil.getUsernameFromToken(token)
          → JwtService.loadUserByUsername(username)
          → JwtUtil.validateToken(token, userDetails)
          → SecurityContextHolder.getContext().setAuthentication(auth)
       → ReservationController.lister()
          → SecurityContextHolder → Authentication → Users courant
          → ReservationService.lister(statut, adherentId, adherent, estBiblio)
```

### c) Flux POST /api/reservations par un ADHERENT (RS-04)
```
Client → POST /api/reservations { "livreId": 202 }
       → JwtRequestFilter → authentification OK
       → WebSecurityConfiguration → /api/reservations/** → authenticated → OK
       → ReservationController.creer(demande)
          → recupererUtilisateurCourant()
             → SecurityContextHolder.getContext().getAuthentication()
             → username = authentication.getName()
             → UsersRepository.findByUsername(username)
          → reservationService.creer(demande, adherent)
             → adherent vient du TOKEN, pas du corps (RS-04)
             → Verification RG-01, RG-02, RG-03
          → Retourne 201 + ReservationResponseDto
```

### d) Flux POST /api/reservations par un BIBLIOTHECAIRE
Identique a (c), sauf que le role est BIBLIOTHECAIRE. Meme logique RS-04 : l'identite vient du token.

### e) Flux GET /api/reservations par un ADHERENT (RS-05)
```
Client → GET /api/reservations
       → ReservationController.lister(statut, adherentId, adherent, estBiblio=false)
       → ReservationService.lister(statut, adherentId=null, adherent, false)
          → RS-05 : adherentId = adherentCourant.getUserId()
          → ReservationRepository.findByAdherentUserIdOrderByDateReservationDesc(userId)
       → Retourne 200 + liste filtree
```

### f) Flux GET /api/reservations par un BIBLIOTHECAIRE
```
Client → GET /api/reservations
       → ReservationController.lister(statut, adherentId, adherent, estBiblio=true)
       → ReservationService.lister(statut, adherentId, adherent, true)
          → Pas de filtrage : toutes les reservations
       → Retourne 200 + liste complete
```

### g) Flux GET /api/reservations/{id} par un ADHERENT (RS-03)
```
Client → GET /api/reservations/5
       → ReservationController.consulter(5)
       → reservationService.consulter(5, adherent, false)
          → reservation = ReservationRepository.findById(5)
          → !estBibliothecaire && reservation.adherent.userId != adherent.userId
          → log.warn("Acces refuse...")
          → throw AccessDeniedException
       → ApiExceptionHandler.accesRefuse()
       → Retourne 403 + ApiError
```

### h) Flux PATCH /api/reservations/{id}/annuler par un ADHERENT (RS-03)
Identique a (g), meme verification du proprietaire dans ReservationService.annuler().

### i) Flux DELETE /api/reservations/{id} par un ADHERENT (RS-02)
```
Client → DELETE /api/reservations/5
       → @PreAuthorize("hasRole('BIBLIOTHECAIRE')")
       → role ADHERENT ne correspond pas
       → AccessDeniedException
       → ApiExceptionHandler.accesRefuse()
       → Retourne 403 + ApiError
```

### j) Flux DELETE /api/reservations/{id} par un BIBLIOTHECAIRE
```
Client → DELETE /api/reservations/5
       → @PreAuthorize("hasRole('BIBLIOTHECAIRE')")
       → role BIBLIOTHECAIRE OK
       → ReservationController.supprimer(5)
       → reservationService.supprimer(5)
       → Retourne 204 No Content
```

### k) Flux d'un token absent (RS-01)
```
Client → GET /api/reservations (pas de header Authorization)
       → JwtRequestFilter : requestTokenHeader == null
       → Pas d'authentification posee dans SecurityContext
       → WebSecurityConfiguration → /api/reservations/** → authenticated → ECHOUE
       → JwtAuthenticationEntryPoint.commence()
       → Retourne 401 Unauthorized
```

### l) Flux d'un token invalide ou expiré
```
Client → GET /api/reservations (Authorization: Bearer token_expire)
       → JwtRequestFilter : JwtUtil.getUsernameFromToken() → ExpiredJwtException
       → username reste null
       → Pas d'authentification dans SecurityContext
       → JwtAuthenticationEntryPoint.commence()
       → Retourne 401 Unauthorized
```

### m) Flux d'un acces refuse (403)
```
Client → DELETE /api/reservations/5 (token ADHERENT valide)
       → @PreAuthorize("hasRole('BIBLIOTHECAIRE')")
       → AccessDeniedException
       → ApiExceptionHandler.accesRefuse()
       → Retourne 403 Forbidden + ApiError { message: "Vous etes authentifie..." }
```

---

## 4. Detail regle par regle

### RS-01 : Sans token, tout endpoint renvoie 401
- **Ou** : `WebSecurityConfiguration.java` — `.antMatchers("/api/reservations/**").authenticated()`
- **Comment** : Spring Security refuse l'acces si aucun Authentication n'est dans le SecurityContext
- **Test** : `getReservationsSansTokenRetourne401()`, `postReservationSansTokenRetourne401()`, etc.

### RS-02 : ADHERENT sur DELETE -> 403
- **Ou** : `ReservationController.java` — `@PreAuthorize("hasRole('BIBLIOTHECAIRE')")` sur `supprimer()`
- **Comment** : annotation Spring Security qui verifie le role avant l'execution de la methode
- **Test** : `adherentTenteDeleteRetourne403v2()`

### RS-03 : ADHERENT sur reservation d'un autre -> 403
- **Ou** : `ReservationService.java` — methodes `consulter()` et `annuler()`
- **Comment** : comparaison `reservation.getAdherent().getUserId().equals(adherentCourant.getUserId())` avec `throw AccessDeniedException` si different
- **Test** : `adherentConsulteReservationDunAutreRetourne403()`, `adherentAnnuleReservationDunAutreRetourne403()`

### RS-04 : Identite depuis le token, pas du corps
- **Ou** : `ReservationRequestDto.java` — champ `adherentId` retire
- **Ou** : `ReservationController.creer()` — `recupererUtilisateurCourant()` lit `SecurityContextHolder`
- **Ou** : `ReservationService.creer(demande, adherent)` — le parametre `adherent` vient du controller
- **Comment** : le SecurityContextHolder est la seule source de verite pour l'identite
- **Test** : `reservationCreeeAuNomDuToken()`

### RS-05 : ADHERENT ne voit que ses reservations
- **Ou** : `ReservationService.lister()` — si `!estBibliothecaire`, `adherentId = adherentCourant.getUserId()`
- **Comment** : filtrage cote serveur, le parametre `adherentId` du client est ecrase
- **Test** : `adherentNeVoitQueSesPropresReservations()`, `bibliothecaireVoitToutesLesReservations()`

---

## 5. Detail des tests

### Test unitaire RG-03 (`ReservationServiceTest.java`)
- **Strategie** : `@ExtendWith(MockitoExtension.class)`, repositories mocks
- `peutCreerUneReservationQuandAdherentADeuxReservationsActives()` : 2 actives → 3e passe
- `refuseCreationQuandAdherentATroisReservationsActives()` : 3 actives → 4e refusee (RG-03)
- `neCompteQueLesStatutsActifs()` : verification que seuls EN_ATTENTE et DISPONIBLE comptent
- Aussi : RG-01, RG-02, RG-04, RG-05, RG-06, RS-03

### Test d'integration (`ReservationSecurityTest.java`)
- **Strategie** : `@SpringBootTest` + `@AutoConfigureMockMvc` + `@WithMockUser` / `SecurityMockMvcRequestPostProcessors.user()`
- RS-01 : 5 tests (GET/POST/PATCH/DELETE sans token → 401)
- RS-02 : 1 test (ADHERENT DELETE → 403)
- RS-03 : 3 tests (consulter/annuler reservation d'un autre → 403)
- RS-04 : 1 test (reservation creee au nom du token)
- RS-05 : 2 tests (ADHERENT filtre, BIBLIOTHECAIRE pas filtre)
- BIBLIOTHECAIRE : 3 tests (consulter/annuler/supprimer → 200/204)

### Sortie console attendue
```
Tests run: X, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## 6. Distinction 401 vs 403

- **401 Unauthorized** : `JwtAuthenticationEntryPoint.commence()` — le serveur ne sait pas qui vous etes (token absent, invalide ou expire)
- **403 Forbidden** : `ApiExceptionHandler.accesRefuse()` — le serveur sait qui vous etes mais votre role ne vous autorise pas

Mecanisme : Spring Security applique d'abord le filtre JWT (authentification), puis les regles d'autorisation. Si l'authentification echoue → 401. Si l'autorisation echoue (role insuffisant, proprietaire different) → AccessDeniedException → 403.

---

## 7. RS-04 en detail

### Avant (faille de securite)
```java
// ReservationRequestDto.java
private Integer adherentId;  // Le client choisit qui il veut etre !

// ReservationController.java
Users adherent = usersRepository.findById(demande.getAdherentId())...;
```
Un attaquant pouvait envoyer `"adherentId": 1` pour creer une reservation au nom de l'admin.

### Apres (correction)
```java
// ReservationRequestDto.java — adherentId RETIRE

// ReservationController.java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String username = auth.getName();
Users adherent = usersRepository.findByUsername(username)...;
reservationService.creer(demande, adherent);
```
L'identite vient du token JWT, impossible a falsifier sans la cle secrete.

---

## 8. Collection Postman

### Chemin
`epreuve/seance-4/bibliotheque-reservation-securite.postman_collection.json`

### Requetes ajoutees
- Authentification ADHERENT et BIBLIOTHECAIRE
- Tests RS-01 (sans token → 401)
- Tests RS-02 (ADHERENT sur DELETE → 403)
- Tests RS-03 (reservation d'un autre → 403)
- Tests RS-04 (POST avec seul livreId)
- Tests RS-05 (filtrage par role)
- Tests BIBLIOTHECAIRE (peut tout faire)

### Variables
- `baseUrl` : http://localhost:8080
- `tokenAdherent` : token JWT de adherent1
- `tokenBiblio` : token JWT de biblio

---

## 9. Jeu de donnees de demo

| Identifiant | Username | Role | Password |
|---|---|---|---|
| 3 | adherent1 | ADHERENT | admin123 |
| 4 | adherent2 | ADHERENT | admin123 |
| 5 | biblio | BIBLIOTHECAIRE | admin123 |

---

## 10. Commandes de test

```bash
cd bibliotheque-backend
./mvnw test
```

### Sortie attendue
```
Tests run: X, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## 11. Livrable

### Branche
`feature/reservation-securite-boubacar-siddighi-balde`

### Messages de commit
1. `securite: configurer l'authentification pour le module reservation (RS-01)`
2. `securite: retirer adherentId du DTO et recuperer l'identite depuis le token (RS-04)`
3. `securite: ajouter les roles ADHERENT et BIBLIOTHECAIRE avec filtrage (RS-03, RS-05)`
4. `securite: restreindre le DELETE aux BIBLIOTHECAIRE (RS-02)`
5. `test: ajouter les tests de securite RS-01 a RS-05`
6. `docs: ajouter le rapport seance 4 et la collection Postman`

### Texte de la Pull Request

**Titre :** Securisation du module Reservation (RS-01 a RS-05)

**Description :**

Ce Pull Request securise l'API de reservation de bibliotheque, auparavant ouverte a tous les utilisateurs (permitAll).

**Regles implementees :**

- **RS-01** : Sans token, tout endpoint de reservation renvoie 401. Implemente dans `WebSecurityConfiguration.java` via `.antMatchers("/api/reservations/**").authenticated()`.
- **RS-02** : Un ADHERENT qui tente un DELETE recoit 403. Implemente via `@PreAuthorize("hasRole('BIBLIOTHECAIRE')")` dans `ReservationController.supprimer()`.
- **RS-03** : Un ADHERENT qui accede a la reservation d'un autre recoit 403. Implemente dans `ReservationService.consulter()` et `ReservationService.annuler()` via comparaison du proprietaire.
- **RS-04** : L'identite de l'adherent vient du token JWT, jamais du corps de la requete. Le champ `adherentId` a ete retire de `ReservationRequestDto`. L'utilisateur est recupere depuis `SecurityContextHolder` dans `ReservationController.creer()`.
- **RS-05** : Un GET par un ADHERENT ne retourne que ses propres reservations. Implemente dans `ReservationService.lister()` via filtrage par `adherentUserId`.

**Tests :**
- Test unitaire RG-03 : repository mocke, aucune BDD requise
- Test d'integration : MockMvc avec @WithMockUser et SecurityMockMvcRequestPostProcessors

**Fichiers modifies :**
- `ReservationRequestDto.java` (retrait adherentId)
- `ReservationService.java` (RS-03, RS-04, RS-05)
- `ReservationController.java` (RS-04, recuperation depuis SecurityContext)
- `WebSecurityConfiguration.java` (RS-01)
- `ReservationSecurityTest.java` (nouveau)
- `ReservationControllerTest.java` (adaptation)
- `ReservationServiceTest.java` (adaptation)
- `docker/seed.sql` (roles ADHERENT, BIBLIOTHECAIRE)

---

## 12. Bonus realises

- Journalisation des tentatives d'acces refusées (`log.warn` dans ReservationService)
- Collection Postman avec scripts de test automatiques

---

## 13. Points de vigilance / limites

- Le secret JWT (`learn_programming_yourself`) est fixe et public : en production, utiliser une variable d'environnement
- La validation de la propriete ne couvre pas le cas ou l'ID dans l'URL est inconnu (404 passe avant 403)
- Les roles ADHERENT/BIBLIOTHECAIRE coexistent avec Admin/User dans la base : pas de migration necessaire

---

## 14. Checklist finale

- [x] Branche creee
- [x] Spring Security configure
- [x] 401/403 distingues
- [x] RS-01 a RS-05 implementees
- [x] Test unitaire RG-03 vert
- [x] Test d'integration vert
- [x] Collection Postman mise a jour et versionnee
- [x] RAPPORT-SEANCE4.md cree a la racine
- [x] Aucune trace de Freebuff dans le code, les commits, la PR, Postman et le rapport
- [x] PR prete
