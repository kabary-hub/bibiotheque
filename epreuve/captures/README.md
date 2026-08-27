# Captures de l'epreuve

| Fichier | Ce qu'il montre |
|---|---|
| `swagger-reservations.png` | La documentation Swagger generee sur `http://localhost:8080/swagger-ui.html`. Les six endpoints du module Reservation y figurent avec leur verbe HTTP et leur resume, sous le tag `Reservations`. C'est la capture demandee dans la description de la PR de la seance 2. |
| `docker-compose-ps.png` | La sortie de `docker compose ps` : les trois services (`bibliotheque-db`, `bibliotheque-backend`, `bibliotheque-frontend`) demarres, la base marquee `healthy` par son healthcheck. C'est la preuve que le `depends_on: condition: service_healthy` de la seance 1 fait son office. |

## Reproduire la capture Swagger

```bash
docker compose up -d db
cd bibliotheque-backend
./mvnw spring-boot:run
```

Puis ouvrir <http://localhost:8080/swagger-ui.html> et deplier le tag `Reservations`.

Les chemins Swagger sont ouverts sans authentification : ils ont ete ajoutes a la
liste `permitAll()` de `WebSecurityConfiguration`. Sans cela, `springdoc` repond 401
et la page reste blanche.
