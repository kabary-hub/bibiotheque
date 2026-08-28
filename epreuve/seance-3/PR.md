# Seance 3 — Ecran de gestion des reservations

## Ce que fait cette PR

Elle donne une interface au module Reservation de la seance 2 : un ecran unique,
accessible depuis la barre de navigation, qui liste les reservations, permet d'en
creer une et d'annuler celles qui peuvent l'etre.

La branche part de la **fusion des seances 1 et 2** : l'ecran a besoin de l'API
de la seance 2, et la demonstration se fait avec le `docker compose` de la
seance 1. Les fichiers de la seance 3 sont les suivants — tout le reste du diff
appartient aux deux seances precedentes, deja relues.

```
bibliotheque-frontend/src/app/_model/reservation.ts
bibliotheque-frontend/src/app/_service/reservation.service.ts
bibliotheque-frontend/src/app/reservations/reservations.component.{ts,html,css}
bibliotheque-frontend/src/app/reservations/reservation-list/*
bibliotheque-frontend/src/app/reservations/reservation-form/*
bibliotheque-frontend/src/app/_auth/auth.interceptor.ts        (modifie)
bibliotheque-frontend/src/app/app.module.ts                    (modifie)
bibliotheque-frontend/src/app/app-routing.module.ts            (modifie)
bibliotheque-frontend/src/app/header/header.component.html     (modifie)
epreuve/seance-3/README.md
```

## Le bug qui rendait l'exercice infaisable

`_auth/auth.interceptor.ts`, ligne 33, avant cette PR :

```ts
return throwError("Some thing is wrong");
```

Toute `HttpErrorResponse` etait remplacee par cette chaine **avant** d'atteindre
le moindre composant. Le corps `ApiError` que la seance 2 prend soin de rediger —
`message`, `regle` — n'arrivait jamais jusqu'a l'ecran.

Autrement dit, le projet produisait deja, pour tous ses ecrans, exactement ce que
l'enonce interdit : « un message generique du type "une erreur est survenue" ».
Aucun affichage lisible d'un 409 n'etait possible sans y toucher.

L'intercepteur relaie desormais l'erreur telle quelle :

```ts
return throwError(() => err);
```

Les redirections 401 → `/login` et 403 → `/forbidden` sont **inchangees**. Seule
la destruction du corps disparait. C'est la seule modification apportee a un
fichier partage par les autres ecrans, et elle leur rend une information qu'ils
n'avaient pas — elle ne leur en retire aucune.

## Un ecran d'administration, et pourquoi

L'enonce demande une colonne « nom de l'adherent » et une liste deroulante
« adherent ». Les deux supposent quelqu'un qui voit les reservations de **tous**
les adherents.

Le code le confirme : `GET /admin/users`, seule source possible pour cette liste,
est annote `@PreAuthorize("hasRole('Admin')")`. Un compte `User` recoit 403 et ne
pourrait pas alimenter le formulaire. La route est donc declaree
`data:{roles:['Admin']}`, comme « Book List » et « User List ».

## Les quatre etats

Ils sont exclusifs — un type somme, pas trois booleens :

```ts
export type EtatListe = 'chargement' | 'donnees' | 'vide' | 'erreur';
```

Le gabarit en affiche un et un seul. Il est structurellement impossible qu'un
tableau vide cohabite avec un indicateur de chargement, ou qu'un message d'erreur
s'affiche au-dessus de donnees valides.

| Etat | Ce que voit l'utilisateur |
|---|---|
| `chargement` | Un `spinner-border` et « Chargement des reservations... », a la place du tableau et de la meme hauteur — l'ecran ne saute pas quand les donnees arrivent |
| `donnees` | Le tableau, ses six colonnes, un badge de couleur par statut |
| `vide` | « Aucune reservation. » — et un second message **different selon qu'un filtre est actif ou non**, parce que « la base est vide » et « ce statut n'a aucune ligne » ne se corrigent pas de la meme facon |
| `erreur` | Le message du serveur et un bouton **Reessayer** qui rejoue l'appel |

L'etat d'erreur a ete verifie comme le demande l'enonce : `docker compose stop
backend`, puis rafraichissement. La requete n'aboutit pas, `status` vaut 0, et
l'ecran affiche « Le serveur est injoignable. Verifiez que le backend est demarre
sur http://localhost:8080, puis reessayez. » Pas un chargement infini.

## Le traitement des refus metier

Le poste le mieux dote du bareme. Le principe tient en une phrase : **le message
affiche est celui du serveur, mot pour mot.**

```ts
const corps = erreur.error as ApiError;
if (corps && corps.message) {
    return new ErreurApi(erreur.status, corps.message, corps.regle);
}
```

Rien n'est reecrit cote client. La phrase de l'API nomme deja sa regle, cite le
titre du livre ou le nom de l'adherent, et donne le quota atteint. La reformuler
ici ferait diverger ce que l'ecran affiche de ce que documente Swagger.

`regle` est lue comme un champ a part et rendue en gras devant le message : c'est
tout l'interet de l'avoir sortie du texte en seance 2.

| Situation | Code | Ce qui s'affiche |
|---|---|---|
| Livre disponible | 409 | `RG-01 — le livre L1 - Le Comte de Monte-Cristo est disponible (3 exemplaire(s) en rayon)...` |
| Reservation deja existante | 409 | `RG-02 — ...` |
| Quota de 3 atteint | 409 | `RG-03 — l'adherent a2_quota a deja 3 reservation(s) active(s)...` |
| Annulation d'un statut terminal | 409 | `RG-05 — ...`, au-dessus de la liste |
| Champ manquant | 400 | Le message de validation du serveur |
| Livre ou adherent inexistant | 404 | Le message du serveur |
| Backend arrete | — | Phrase dediee, ecrite cote client : le serveur n'a rien pu dire |

Aucun `alert()`, aucun `console.log` en guise de traitement, aucun echec
silencieux : le bouton passe en « Creation... » pendant l'appel et l'issue est
toujours affichee, succes comme echec.

### Le 400 est devenu difficile a provoquer, et c'est voulu

Le bouton reste inactif tant que les deux listes deroulantes ne sont pas
renseignees — c'est la validation demandee. Un corps incomplet ne peut donc plus
partir de cet ecran. Le chemin 400 reste code et teste, mais il se demontre par
Swagger ou par la collection Postman de la seance 2, pas depuis l'interface.

Le 404, lui, se provoque en supprimant un livre depuis « Book List » sans
recharger l'ecran des reservations : la liste deroulante propose alors un
identifiant que le serveur ne connait plus.

## Une decision qui merite d'etre discutee

**La liste des livres n'est pas restreinte aux livres indisponibles.**

Il aurait ete tentant de n'y mettre que les livres a 0 exemplaire, les seuls
reservables. C'aurait ete une erreur : la regle RG-01 serait alors appliquee par
le navigateur, le serveur ne serait plus jamais consulte sur ce point, et le
message qu'il prend soin de rediger ne s'afficherait jamais. Le refus le mieux
dote du bareme deviendrait inatteignable depuis l'ecran.

Le nombre d'exemplaires est donc affiche dans chaque option — « L1 - Le Comte de
Monte-Cristo — Alexandre Dumas (3 exemplaire(s) en rayon) » — pour que le choix
soit eclaire, et le refus vient du serveur.

## L'annulation

- Le bouton n'apparait que pour `EN_ATTENTE` et `DISPONIBLE`, lus depuis
  `STATUTS_ANNULABLES` — la meme liste que `StatutReservation.ACTIFS` cote serveur.
  Un statut terminal affiche un tiret, pas une cellule vide qui ressemblerait a
  un bug.
- Une modale de confirmation nomme la reservation, l'adherent et le livre, et
  previent que la transition est definitive.
- C'est une modale maison : le JavaScript de Bootstrap n'est pas dans le bundle
  (`angular.json` ne charge que jQuery), et un `confirm()` natif bloque le
  navigateur, comme l'`alert()` que l'enonce interdit.
- En cas de succes, **la ligne est remplacee par la reservation renvoyee par le
  serveur**, pas rechargee ni modifiee a la main : le statut affiche est celui
  que la base contient reellement.

## Un detail qui aurait ressemble a un bug

Une reservation nait `EN_ATTENTE`. Si un filtre sur un autre statut etait actif
au moment de la creation, la ligne nouvellement creee n'apparaitrait pas dans la
liste rechargee — l'ecran semblerait n'avoir rien fait, alors que le serveur a
bien repondu 201. Le filtre revient donc a « Tous » apres une creation reussie.

## Architecture

- **Un service dedie.** `HttpClient` n'est injecte que dans
  `_service/reservation.service.ts`, `books.service.ts` et `users.service.ts`.
  Aucun composant n'en voit un.
- **Trois composants.** Un conteneur qui detient l'etat et declenche les appels ;
  une liste et un formulaire purement presentationnels, qui recoivent des `@Input`
  et emettent des `@Output`. Tous deux se rendent avec n'importe quel jeu de
  donnees, sans backend.
- **Aucune donnee codee en dur.** Livres, adherents et reservations viennent tous
  de l'API. Seul le vocabulaire des statuts est declare cote client : c'est le
  contrat de l'API, au meme titre que le nom des champs.
- **Interface en francais**, y compris l'entree de menu. Le reste de la barre de
  navigation est en anglais (« Book List », « Add Book ») : l'enonce demande le
  francais *et* la coherence avec le projet, les deux ne peuvent pas etre tenus
  a la fois. J'ai choisi de respecter la langue demandee sur mon perimetre plutot
  que de traduire des ecrans qui ne sont pas l'objet de cette PR.

## Captures

<!-- Glisser-deposer ici : chargement, liste remplie, liste vide, refus 409. -->

> Les quatre captures demandees doivent etre **glissees-deposees dans l'editeur
> de la Pull Request**. Un chemin relatif vers `epreuve/seance-3/captures/`
> s'affiche dans le depot mais pas dans une description de PR : GitHub n'y resout
> pas les chemins relatifs.

## Verifier en local

```powershell
docker compose up --build -d
Get-Content epreuve\seance-2\fixtures.sql | docker exec -i bibliotheque-db mysql -u root -pmysql bibliotheque
```

Puis <http://localhost:4200>, connexion `admin` / `admin123`, menu
**Reservations**. `epreuve/seance-3/README.md` donne la manipulation exacte pour
provoquer chacun des cas ci-dessus.
