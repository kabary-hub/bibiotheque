# Séance 1 — Dockerisation et rapport d'analyse

> **Branche :** `epreuve/tasse-ulruch` → `main`
> **À coller dans la description de la Pull Request.**

---

## Ce que fait cette PR

Elle rend le projet **Gestion de Bibliothèque** entièrement exécutable par
`docker compose up`, sur une machine qui n'a jamais vu le projet, et documente
l'analyse demandée dans `epreuve/RAPPORT.md`.

## Le problème résolu

Sur un poste à jour, le projet **ne compilait pas et ne se construisait pas** :

| Composant | Ce que le projet exige | Ce qui est installé | Conséquence |
|---|---|---|---|
| Backend | Lombok 1.18.20 → JDK ≤ 17 | JDK 21 | `NoSuchFieldError` sur `JCTree.qualid` |
| Frontend | Angular 14 → Node 14/16 | Node 24 | build non fiable |
| Base | MySQL 8 sur `localhost:3306` | rien | `Communications link failure` |

Aucune de ces corrections ne pouvait passer par `pom.xml`, `package.json` ou
`application.properties`, interdits de modification. **La solution est donc
entièrement dans les images** : chaque service embarque la version d'outillage
dont il a besoin, sans rien exiger du poste hôte.

## Ce qui a été ajouté

| Fichier | Rôle |
|---|---|
| `docker-compose.yml` | Les quatre services : `db`, `backend`, `frontend`, `seed` |
| `bibliotheque-backend/Dockerfile` | Build multi-étapes : `maven:3.8-eclipse-temurin-11` → `eclipse-temurin:11-jre` |
| `bibliotheque-frontend/Dockerfile` | `node:16-alpine` pour compiler → `nginx:alpine` pour servir |
| `bibliotheque-frontend/nginx.conf` | Routage Angular côté navigateur, sinon `/books` renvoie 404 au rechargement |
| `docker/seed.sql` + `seed-entrypoint.sh` | Jeu de données initial : sans lui, la base est vide et **on ne peut pas entrer dans l'application** |
| `bibliotheque-backend/maven-settings.xml` | Miroir vers Maven Central |
| `.env.example` | Variables surchargeables, aucun mot de passe en dur |
| `.gitattributes` | `*.sh text eol=lf` — sans quoi un shebang CRLF donne `exec format error` |
| `.dockerignore` × 2 | Empêche `target/` et `node_modules/` d'entrer dans le contexte de build |

## Le rapport

`epreuve/RAPPORT.md` — 917 lignes, structuré selon les cinq parties demandées :

1. **Environnement** — versions relevées, le Java réellement utilisé par Maven, et l'échec de build avec sa sortie brute
2. **Arborescence** — paramètres de connexion, ce qui rend `/authenticate` public, le passage entité → table, qui écrit le corps de `save()`, l'adresse codée en dur, routes et rôles
3. **Docker** — l'écart de JDK, la surcharge sans toucher aux `.properties`, l'attente réelle de la base, la persistance, le piège de `localhost:8080`, la modification jugée nécessaire mais volontairement non faite, le service `seed`
4. **Trajet** — l'emprunt d'un livre du clic à la base, trois manipulations avec leurs codes HTTP réels, la double protection
5. **Bonus** — un défaut du code existant, son risque et sa correction

## Trois points qui méritent l'attention du relecteur

**Le faux coupable de l'échec de build.** Le message
`warning: [options] source value 8 is obsolete` apparaît juste au-dessus de
l'erreur. Ce n'est qu'un avertissement : la cause réelle est que Lombok 1.18.20
attaque les classes internes de `javac`, dont le champ `qualid` a disparu au
JDK 21. Le rapport le documente explicitement (§1.3).

**Une condition de course dans le service `seed`.** Attendre la seule table
`books` laissait passer le script alors que `users` n'existait pas encore,
d'où un `ERROR 1146` en pleine insertion. `seed-entrypoint.sh` attend
désormais les quatre tables. Invisible tant que l'ordre de création
d'Hibernate reste favorable — c'est précisément ce qui la rendait dangereuse.

**La modification jugée nécessaire, et non faite.** Conformément à la consigne,
elle est décrite dans le rapport (§3.6) plutôt qu'appliquée.

## Vérification

```powershell
docker compose build --no-cache
docker compose up -d
```

Puis <http://localhost:4200>, connexion `admin` / `admin123`.

Les codes HTTP de la section 4.2 ont été **confirmés par des appels réels**, pas
déduits de la lecture du code.

---

## Reste à faire avant d'ouvrir la PR

- [ ] Ajouter les captures dans `epreuve/captures/` — le dossier est encore vide :
      `docker-compose-ps.png` et `application.png`.
