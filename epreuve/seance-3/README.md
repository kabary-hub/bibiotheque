# Seance 3 — Ecran de gestion des reservations

Interface Angular du module livre en seance 2. Un ecran unique, reserve au role
**Admin**, accessible depuis la barre de navigation sous « Reservations ».

## Pourquoi un ecran d'administration

L'enonce demande une colonne « nom de l'adherent » et une liste deroulante
« adherent » dans le formulaire. Les deux n'ont de sens que pour quelqu'un qui
voit les reservations de **tous** les adherents : un ecran self-service
n'afficherait pas le nom de son propre utilisateur dans chaque ligne, et ne lui
demanderait pas de se choisir lui-meme.

Le code confirme ce decoupage : `GET /admin/users`, la seule source possible pour
la liste des adherents, est annote `@PreAuthorize("hasRole('Admin')")`. Un compte
`User` recoit 403 et n'aurait aucun moyen d'alimenter cette liste.

## Ce qui compose l'ecran

| Fichier | Role |
|---|---|
| `_service/reservation.service.ts` | **Le seul** endroit ou `HttpClient` touche `/api/reservations`. Porte aussi `messageDe()`, qui traduit une erreur en message affichable. |
| `_model/reservation.ts` | Le contrat cote client : `Reservation`, `ApiError`, le vocabulaire des statuts. |
| `reservations/reservations.component.*` | Le conteneur. Detient l'etat, declenche les appels, arbitre les quatre etats. |
| `reservations/reservation-list/*` | Le tableau. Ne connait ni service ni etat : il recoit des lignes et emet une demande d'annulation. |
| `reservations/reservation-form/*` | Le formulaire. Recoit les deux referentiels, emet le couple choisi, affiche l'erreur qu'on lui repasse. |

## Demarrer

```powershell
docker compose up --build -d
```

Puis <http://localhost:4200>, connexion **`admin` / `admin123`**, menu
**Reservations**.

## Le jeu de donnees de la demonstration

L'enonce demande « une base contenant un livre disponible et plusieurs livres
empruntes ». C'est exactement ce que produit `epreuve/seance-2/fixtures.sql`, qui
n'est donc pas duplique ici :

```powershell
Get-Content epreuve\seance-2\fixtures.sql | docker exec -i bibliotheque-db mysql -u root -pmysql bibliotheque
```

| Element | Ce qu'il permet de montrer |
|---|---|
| **L1** — Le Comte de Monte-Cristo, 3 exemplaires | Le refus **RG-01** : un livre en rayon ne se reserve pas |
| **L2 a L5** — 0 exemplaire, empruntes par A3 | Les creations qui reussissent |
| **A2** — « Saturera son quota » | Le refus **RG-03** a la 4e reservation |

## Provoquer chaque cas devant le formateur

| Ce qu'on veut voir | Manipulation |
|---|---|
| Chargement | Recharger la page. Pour le rendre visible sur un backend local, brider le reseau (DevTools → Network → Slow 3G) |
| Liste remplie | Creer deux ou trois reservations sur L2, L3, L4 |
| Liste vide | Filtrer sur **Honoree** : aucune reservation ne porte ce statut |
| Erreur | `docker compose stop backend`, puis « Rafraichir » |
| **409 RG-01** | Reserver **L1** (3 exemplaires en rayon) |
| **409 RG-02** | Reserver deux fois L2 pour le meme adherent |
| **409 RG-03** | Donner 3 reservations actives a A2, puis en tenter une 4e |
| **409 RG-05** | Annuler une reservation deja annulee — le bouton disparait, mais l'API repond 409 si l'etat a change dans un autre onglet |
| **400** | Le bouton reste inactif tant qu'un champ manque : le 400 ne peut plus venir de l'ecran. Il est demontrable via Swagger ou la collection Postman de la seance 2 |
| **404** | Supprimer un livre depuis « Book List » sans recharger l'ecran des reservations, puis le reserver |

## Une modification de l'existant

`_auth/auth.interceptor.ts` remplacait toute erreur HTTP par la chaine
`"Some thing is wrong"` avant qu'aucun composant ne la voie. Le corps `ApiError`
n'arrivait jamais : impossible d'afficher le message du serveur, impossible de
distinguer un 409 metier d'un backend arrete. L'intercepteur relaie desormais
l'erreur telle quelle (`throwError(() => err)`). Les redirections 401 et 403 sont
inchangees.
