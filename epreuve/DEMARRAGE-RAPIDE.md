# Démarrage rapide

Du clone à l'application ouverte, sur une machine où seul **Docker Desktop** est
installé. Ni JDK, ni Node, ni MySQL ne sont nécessaires : tout est construit
dans des conteneurs.

```bash
git clone -b epreuve/tasse-ulruch https://github.com/KFOKAM48/bibiotheque.git
cd bibiotheque
docker compose up -d --build
```

Puis ouvrir **<http://localhost:4200>** et se connecter :

| Identifiant | Mot de passe | Rôle |
|---|---|---|
| `admin` | `admin123` | `Admin` — gestion du catalogue et des comptes |
| `lecteur` | `admin123` | `User` — emprunt et restitution |

Les deux comptes et six livres sont insérés automatiquement par le service
`seed` au premier démarrage. Aucun SQL à taper.

---

## Ce qui tourne

| Service | Adresse | Rôle |
|---|---|---|
| `frontend` | <http://localhost:4200> | Interface Angular, servie par nginx |
| `backend` | <http://localhost:8080> | API Spring Boot |
| `db` | `localhost:3306` | MySQL 8, schéma `bibliotheque` |
| `seed` | — | Insère les données initiales, puis s'arrête |

> **Le premier `up` est long** : environ 15 à 25 minutes selon la connexion.
> Maven et npm téléchargent l'intégralité de leurs dépendances. Les
> démarrages suivants prennent quelques secondes.
>
> `seed` apparaît en `Exited (0)` dans `docker compose ps -a` — c'est normal,
> c'est une tâche ponctuelle et non un service.

---

## Vérifier que tout répond

```bash
docker compose ps                      # trois services « running », seed « exited (0) »
curl http://localhost:8080/authenticate \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin123"}'
```

La seconde commande doit renvoyer un JSON contenant `jwtToken`.

---

## Arrêter

```bash
docker compose down        # arrête tout, conserve les données
docker compose down -v     # supprime aussi la base
```
