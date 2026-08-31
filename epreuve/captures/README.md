# Captures de l'epreuve — seance 1

| Fichier | Ce qu'il montre |
|---|---|
| `docker-compose-ps.png` | La sortie de `docker compose ps` : les trois services (`bibliotheque-db`, `bibliotheque-backend`, `bibliotheque-frontend`) demarres, la base marquee `healthy` par son healthcheck. C'est la preuve que le `depends_on: condition: service_healthy` fait son office — le backend attend la **disponibilite** de MySQL, pas seulement le demarrage du conteneur. |

## Reproduire

```bash
docker compose up -d
docker compose ps
```

La base met plusieurs dizaines de secondes a initialiser ses fichiers au tout premier
lancement. Tant qu'elle n'est pas `healthy`, le backend n'est pas demarre : c'est le
comportement attendu, pas un blocage.

> La capture Swagger du module Reservation appartient a la **seance 2**. Elle vit sur la
> branche `feature/reservation-tasse-ulruch`, sous `epreuve/seance-2/captures/`.
