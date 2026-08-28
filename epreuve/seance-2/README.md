# Séance 2 — livrable du module Réservation

| Fichier | À quoi il sert |
|---|---|
| `PR.md` | La description de la Pull Request, à coller telle quelle. Contient la capture Swagger et le tableau qui dit, en une phrase et avec son emplacement, où chaque règle RG-01 à RG-06 est implémentée. |
| `captures/swagger-reservations.png` | La documentation Swagger générée sur `http://localhost:8080/swagger-ui.html`, tag `Reservations`. Les six endpoints avec leur verbe et leur résumé. |
| `fixtures.sql` | Le jeu de données de test : L1 disponible, L2 à L5 tous empruntés et non rendus, et les trois adhérents A1 (réservataire), A2 (saturera son quota), A3 (l'emprunteur qui détient L2 à L5). Rejouable sans créer de doublons. |
| `bibliotheque-reservation.postman_collection.json` | 40 requêtes, 79 assertions, dans l'ordre d'exécution. L'authentification est en position 1 avec les identifiants déjà remplis ; le Collection Runner enchaîne tout sans intervention. |

Le code du module est sous
`bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/` — `entity`, `dao`, `dto`,
`service`, `controller`, `exceptions`.

## Mise en route

```powershell
# 1. La base
docker compose up -d db          # ou : docker start bibliotheque-db

# 2. L'application
cd bibliotheque-backend
.\mvnw.cmd clean package -DskipTests
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3306/bibliotheque?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC"
java -jar target\bibliotheque-0.0.1-SNAPSHOT.jar
```

Hibernate crée la table `reservation` au démarrage (`ddl-auto=update`). Charger ensuite le jeu
de données :

```powershell
docker exec -i bibliotheque-db mysql -uroot -pmysql bibliotheque < epreuve\seance-2\fixtures.sql
```

Puis importer la collection dans Postman et lancer le Runner.

## Les cinq scénarios que la collection démontre

1. **Parcours nominal** — A1 réserve L2, consulte, filtre, annule.
2. **Les six règles** — RG-01 sur L1 qui est disponible, RG-02 en réservant deux fois le même
   livre, RG-03 en saturant le quota de A2 puis en vérifiant qu'une annulation le libère,
   RG-05 et RG-06 en annulant deux fois de suite.
3. **Validation** — champ manquant, corps vide, statut inconnu dans l'URL.
4. **Ressources absentes** — livre, adhérent et réservation inexistants, sur les trois verbes.
5. **Sécurité** — 401 sans jeton, 403 avec un jeton d'adhérent simple sur une route d'admin.

## Pourquoi `no_of_copies = 0`

RG-01 ne regarde que cette colonne : un livre à zéro exemplaire est indisponible, donc réservable.
Les quatre emprunts en cours de `fixtures.sql` (`return_date IS NULL`) sont ce qui rend la
situation cohérente — les exemplaires ne sont pas perdus, ils sont chez A3.
