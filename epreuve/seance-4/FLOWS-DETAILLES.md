# Flows Complets Detailles — Seance 4

## Flow 1 : Authentification (login → generation du token)

```
Client                          Backend
  │                               │
  │  POST /authenticate            │
  │  { username, password }        │
  │  ─────────────────────────────►│
  │                               │
  │                    JwtController.createJwtToken()
  │                               │
  │                    JwtService.createJwtToken()
  │                      │
  │                      ├── AuthenticationManager.authenticate()
  │                      │     └── Verifie le mot de passe BCrypt
  │                      │
  │                      ├── JwtService.loadUserByUsername()
  │                      │     └── UsersRepository.findByUsername()
  │                      │         → charge l'utilisateur + ses roles
  │                      │         → construit les autorites :
  │                      │           ROLE_ADHERENT ou ROLE_BIBLIOTHECAIRE
  │                      │
  │                      └── JwtUtil.generateToken(userDetails)
  │                            → genere un token HS512
  │                            → subject = username
  │                            → expiration = 5 heures
  │                               │
  │  ◄─────────────────────────────│
  │  JwtResponse {                 │
  │    user: { userId, username,   │
  │            name, role },       │
  │    jwtToken: "eyJhbGci..."     │
  │  }                             │
  │                               │
  │  Status: 200 OK               │
```

---

## Flow 2 : Requete authentifiee (token → filtre → SecurityContext → controleur)

```
Client                          JwtRequestFilter              Controleur
  │                               │                             │
  │  GET /api/reservations         │                             │
  │  Authorization: Bearer xxx     │                             │
  │  ─────────────────────────────►│                             │
  │                               │                             │
  │                    doFilterInternal()                        │
  │                      │                                       │
  │                      ├── Extrait le token du header           │
  │                      │   "Bearer xxx" → "xxx"                │
  │                      │                                       │
  │                      ├── JwtUtil.getUsernameFromToken()       │
  │                      │   → lit le claim "sub" du JWT         │
  │                      │   → retourne le username              │
  │                      │                                       │
  │                      ├── JwtService.loadUserByUsername()      │
  │                      │   → charge l'utilisateur depuis la BDD│
  │                      │   → construit les UserDetails          │
  │                      │                                       │
  │                      ├── JwtUtil.validateToken()              │
  │                      │   → verifie que le token n'est pas     │
  │                      │     expire et que le username matche   │
  │                      │                                       │
  │                      ├── SecurityContextHolder               │
  │                      │     .getContext()                      │
  │                      │     .setAuthentication(auth)           │
  │                      │   → pose l'utilisateur dans le contexte│
  │                      │     de securite                        │
  │                      │                                       │
  │                               ──────────────────────────────►│
  │                               │                              │
  │                               │   Controleur recoit la       │
  │                               │   requete avec l'utilisateur │
  │                               │   deja authentifie dans      │
  │                               │   SecurityContextHolder      │
```

---

## Flow 3 : POST /api/reservations par un ADHERENT (RS-04)

```
Client                          Controleur                     Service
  │                               │                              │
  │  POST /api/reservations        │                              │
  │  Authorization: Bearer xxx     │                              │
  │  Body: { "livreId": 202 }      │                              │
  │  (PAS de adherentId !)         │                              │
  │  ─────────────────────────────►│                              │
  │                               │                              │
  │                    WebSecurityConfiguration                   │
  │                      → /api/reservations/** → authenticated  │
  │                      → Token valide → OK                      │
  │                               │                              │
  │                    ReservationController.creer(demande)       │
  │                      │                                       │
  │                      ├── recupererUtilisateurCourant()        │
  │                      │     SecurityContextHolder             │
  │                      │       .getContext()                    │
  │                      │       .getAuthentication()             │
  │                      │     → username = "adherent1"           │
  │                      │     → UsersRepository.findByUsername() │
  │                      │     → Users { userId: 3, ... }        │
  │                      │                                       │
  │                      │         ─────────────────────────────►│
  │                      │         │                              │
  │                      │         │  reservationService.creer(   │
  │                      │         │    demande, adherent)        │
  │                      │         │                              │
  │                      │         │  Note: adherent vient du     │
  │                      │         │  TOKEN, pas du DTO           │
  │                      │         │  (RS-04)                     │
  │                      │         │                              │
  │                      │         │  ├── Verifie RG-01           │
  │                      │         │  │   (livre indisponible?)   │
  │                      │         │  ├── Verifie RG-02           │
  │                      │         │  │   (pas de doublon actif?) │
  │                      │         │  ├── Verifie RG-03           │
  │                      │         │  │   (moins de 3 actives?)  │
  │                      │         │  ├── Calcule dates (RG-04)   │
  │                      │         │  │   dateReservation = now() │
  │                      │         │  │   dateExpiration = +7j    │
  │                      │         │  └── reservationRepository   │
  │                      │         │        .save(reservation)    │
  │                      │         │                              │
  │                      │         ◄──────────────────────────────│
  │                      │         │                              │
  │  ◄─────────────────────────────│                              │
  │  {                             │                              │
  │    "id": 4,                    │                              │
  │    "livreId": 202,             │                              │
  │    "livreTitre": "Germinal",   │                              │
  │    "adherentId": 3,  ◄── ID du TOKEN, pas du client          │
  │    "adherentNom": "Adherent Alpha",                           │
  │    "statut": "EN_ATTENTE",     │                              │
  │    "dateReservation": "...",   │                              │
  │    "dateExpiration": "...+7j"  │                              │
  │  }                             │                              │
  │  Status: 201 Created           │                              │
```

---

## Flow 4 : GET /api/reservations par un ADHERENT (RS-05 : filtrage)

```
Client                          Controleur                     Service
  │                               │                              │
  │  GET /api/reservations         │                              │
  │  Authorization: Bearer xxx     │                              │
  │  ─────────────────────────────►│                              │
  │                               │                              │
  │                    ReservationController.lister()             │
  │                      │                                       │
  │                      ├── adherent = recupererUtilisateurCourant()
  │                      │     → username = "adherent1"           │
  │                      │     → Users { userId: 3 }             │
  │                      │                                       │
  │                      ├── estBiblio = aLeRole("BIBLIOTHECAIRE")
  │                      │     → false (role est ADHERENT)        │
  │                      │                                       │
  │                      │         ─────────────────────────────►│
  │                      │         │                              │
  │                      │         │  reservationService.lister(  │
  │                      │         │    statut, adherentId,       │
  │                      │         │    adherent, estBiblio=false) │
  │                      │         │                              │
  │                      │         │  RS-05 : si (!estBiblio)     │
  │                      │         │    adherentId = adherent     │
  │                      │         │      .getUserId()            │
  │                      │         │    → adherentId = 3          │
  │                      │         │                              │
  │                      │         │  SELECT * FROM reservation   │
  │                      │         │  WHERE adherent_id = 3       │
  │                      │         │  ORDER BY date_reservation   │
  │                      │         │    DESC                      │
  │                      │         │                              │
  │                      │         ◄──────────────────────────────│
  │                      │         │                              │
  │  ◄─────────────────────────────│                              │
  │  [                            │                              │
  │    { "id": 4, "adherentId": 3 },  ◄── que les siennes        │
  │    ...                          │                              │
  │  ]                             │                              │
  │  Status: 200 OK                │                              │
```

---

## Flow 5 : GET /api/reservations par un BIBLIOTHECAIRE (pas de filtrage)

```
Client                          Controleur                     Service
  │                               │                              │
  │  GET /api/reservations         │                              │
  │  Authorization: Bearer xxx     │                              │
  │  ─────────────────────────────►│                              │
  │                               │                              │
  │                    ReservationController.lister()             │
  │                      │                                       │
  │                      ├── adherent = recupererUtilisateurCourant()
  │                      │     → username = "biblio"              │
  │                      │                                       │
  │                      ├── estBiblio = aLeRole("BIBLIOTHECAIRE")
  │                      │     → true                             │
  │                      │                                       │
  │                      │         ─────────────────────────────►│
  │                      │         │                              │
  │                      │         │  reservationService.lister(  │
  │                      │         │    statut, adherentId,       │
  │                      │         │    adherent, estBiblio=true)  │
  │                      │         │                              │
  │                      │         │  Pas de filtrage :           │
  │                      │         │  SELECT * FROM reservation   │
  │                      │         │  ORDER BY date_reservation   │
  │                      │         │    DESC                      │
  │                      │         │                              │
  │                      │         ◄──────────────────────────────│
  │                      │         │                              │
  │  ◄─────────────────────────────│                              │
  │  [                            │                              │
  │    { "id": 1, "adherentId": 2 },  ◄── toutes les resas       │
  │    { "id": 4, "adherentId": 3 },     de TOUS les adherents   │
  │    ...                          │                              │
  │  ]                             │                              │
  │  Status: 200 OK                │                              │
```

---

## Flow 6 : GET /api/reservations/{id} par un ADHERENT (RS-03 : proprietaire)

```
Client                          Controleur                     Service
  │                               │                              │
  │  GET /api/reservations/5       │                              │
  │  Authorization: Bearer xxx     │                              │
  │  (adherent1 = userId 3)       │                              │
  │  ─────────────────────────────►│                              │
  │                               │                              │
  │                    ReservationController.consulter(5)         │
  │                      │                                       │
  │                      ├── adherent = recupererUtilisateurCourant()
  │                      │     → userId: 3, username: "adherent1" │
  │                      │                                       │
  │                      ├── estBiblio = false                    │
  │                      │                                       │
  │                      │         ─────────────────────────────►│
  │                      │         │                              │
  │                      │         │  reservationService          │
  │                      │         │    .consulter(5, adherent,   │
  │                      │         │      false)                  │
  │                      │         │                              │
  │                      │         │  reservation = findById(5)   │
  │                      │         │  → reservation.adherent      │
  │                      │         │      .userId = 4 ◄── pas 3 ! │
  │                      │         │                              │
  │                      │         │  !estBiblio (true)           │
  │                      │         │  && 4 != 3                   │
  │                      │         │  → log.warn("Acces refuse")  │
  │                      │         │  → throw AccessDeniedException│
  │                      │         │                              │
  │                      │         ◄──────────────────────────────│
  │                      │         │                              │
  │                    ApiExceptionHandler.accesRefuse()           │
  │                      → 403 Forbidden                           │
  │                               │                              │
  │  ◄─────────────────────────────│                              │
  │  {                             │                              │
  │    "status": 403,              │                              │
  │    "error": "Forbidden",       │                              │
  │    "message": "Vous etes       │                              │
  │      authentifie mais votre    │                              │
  │      role ne permet pas cette  │                              │
  │      operation."               │                              │
  │  }                             │                              │
  │  Status: 403 Forbidden         │                              │
```

---

## Flow 7 : DELETE /api/reservations/{id} par un ADHERENT (RS-02 : 403)

```
Client                          Controleur
  │                               │
  │  DELETE /api/reservations/5    │
  │  Authorization: Bearer xxx     │
  │  (role = ADHERENT)             │
  │  ─────────────────────────────►│
  │                               │
  │                    @PreAuthorize("hasRole('BIBLIOTHECAIRE')")
  │                      │
  │                      ├── Verifie les autorites de l'utilisateur
  │                      │   → ROLE_ADHERENT ne matche pas
  │                      │     ROLE_BIBLIOTHECAIRE
  │                      │
  │                      └── throw AccessDeniedException
  │                               │
  │                    ApiExceptionHandler.accesRefuse()
  │                               │
  │  ◄─────────────────────────────│
  │  {                             │
  │    "status": 403,              │
  │    "error": "Forbidden",       │
  │    "message": "Vous etes       │
  │      authentifie mais votre    │
  │      role ne permet pas cette  │
  │      operation."               │
  │  }                             │
  │  Status: 403 Forbidden         │
```

---

## Flow 8 : DELETE /api/reservations/{id} par un BIBLIOTHECAIRE (OK)

```
Client                          Controleur                     Service
  │                               │                              │
  │  DELETE /api/reservations/5    │                              │
  │  Authorization: Bearer xxx     │                              │
  │  (role = BIBLIOTHECAIRE)       │                              │
  │  ─────────────────────────────►│                              │
  │                               │                              │
  │                    @PreAuthorize("hasRole('BIBLIOTHECAIRE')")
  │                      │                                       │
  │                      ├── Verifie les autorites               │
  │                      │   → ROLE_BIBLIOTHECAIRE → OK          │
  │                      │                                       │
  │                      │         ─────────────────────────────►│
  │                      │         │                              │
  │                      │         │  reservationService          │
  │                      │         │    .supprimer(5)             │
  │                      │         │                              │
  │                      │         │  reservation = findById(5)   │
  │                      │         │  reservationRepository       │
  │                      │         │    .delete(reservation)      │
  │                      │         │                              │
  │                      │         ◄──────────────────────────────│
  │                      │         │                              │
  │  ◄─────────────────────────────│                              │
  │  Status: 204 No Content        │                              │
```

---

## Flow 9 : Token absent (RS-01 : 401)

```
Client                          JwtRequestFilter     WebSecurityConfig
  │                               │                    │
  │  GET /api/reservations         │                    │
  │  (pas de header Authorization) │                    │
  │  ─────────────────────────────►│                    │
  │                               │                    │
  │                    doFilterInternal()                │
  │                      │                              │
  │                      ├── requestTokenHeader = null   │
  │                      │   → Pas de token              │
  │                      │   → username = null            │
  │                      │                              │
  │                      ├── Pas de SecurityContext      │
  │                      │   rempli                      │
  │                      │                              │
  │                               ─────────────────────►│
  │                               │                     │
  │                               │  authorizeRequests()│
  │                               │  /api/reservations/ │
  │                               │  ** → authenticated│
  │                               │                     │
  │                               │  Aucun Authentica-  │
  │                               │  tion dans le       │
  │                               │  SecurityContext    │
  │                               │                     │
  │                               │  → ECHOUE           │
  │                               │                     │
  │                    JwtAuthenticationEntryPoint       │
  │                      .commence()                     │
  │                      → 401 Unauthorized              │
  │                               │                     │
  │  ◄─────────────────────────────│                     │
  │  {                             │                     │
  │    "status": 401,              │                     │
  │    "error": "Unauthorized"     │                     │
  │  }                             │                     │
```

---

## Flow 10 : Token invalide ou expire (401)

```
Client                          JwtRequestFilter
  │                               │
  │  GET /api/reservations         │
  │  Authorization: Bearer xxx     │
  │  (token expire)                │
  │  ─────────────────────────────►│
  │                               │
  │                    doFilterInternal()
  │                      │
  │                      ├── Extrait le token
  │                      │
  │                      ├── JwtUtil.getUsernameFromToken()
  │                      │   → ExpiredJwtException !
  │                      │   → username reste null
  │                      │
  │                      ├── Pas d'authentification posee
  │                      │   dans SecurityContext
  │                      │
  │                    JwtAuthenticationEntryPoint
  │                      .commence()
  │                      → 401 Unauthorized
  │                               │
  │  ◄─────────────────────────────│
  │  { "status": 401, "error": "Unauthorized" }
```
