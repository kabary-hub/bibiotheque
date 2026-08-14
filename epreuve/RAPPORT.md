# Rapport — Épreuve séance 1

| | |
|---|---|
| **Auteur** | Tasse Ulruch <!-- vérifie l'orthographe exacte de ton nom --> |
| **Branche** | `epreuve/tasse-ulruch` |
| **Machine** | Windows 11 Pro, terminal PowerShell |
| **Date** | 14 août 2026 |

---

## 1. Environnement

### 1.1 — Versions installées

Sorties brutes des cinq commandes demandées, telles qu'obtenues dans PowerShell
à la racine du dépôt.

```
PS C:\Users\bobot\Desktop\bibiotheque> java -version
java version "21.0.10" 2026-01-20 LTS
Java(TM) SE Runtime Environment (build 21.0.10+8-LTS-217)
Java HotSpot(TM) 64-Bit Server VM (build 21.0.10+8-LTS-217, mixed mode, sharing)

PS C:\Users\bobot\Desktop\bibiotheque> node -v
v24.13.0

PS C:\Users\bobot\Desktop\bibiotheque> npm -v
11.17.0

PS C:\Users\bobot\Desktop\bibiotheque> docker compose version
Docker Compose version v5.3.1

PS C:\Users\bobot\Desktop\bibiotheque> git --version
git version 2.55.0.windows.4
```

Vérification complémentaire que le démon Docker tourne réellement — `docker -v`
répond même lorsque Docker Desktop est éteint, seul `docker ps` en fait la
preuve :

```
PS C:\Users\bobot\Desktop\bibiotheque> docker ps
CONTAINER ID   IMAGE     COMMAND   CREATED   STATUS    PORTS     NAMES
```

Le tableau est vide (aucun conteneur lancé) mais les colonnes s'affichent sans
erreur de connexion : le démon répond.

#### Synthèse

| Outil | Version relevée | Minimum exigé | Statut |
|---|---|---|---|
| JDK | 21.0.10 LTS (Oracle) | 17 | conforme |
| Node.js | 24.13.0 | 20 | conforme |
| npm | 11.17.0 | — | — |
| Docker Compose | v5.3.1 | à jour et lancé | conforme |
| Git | 2.55.0.windows.4 | — | — |

---

### 1.2 — Le Java de Maven

Sortie brute de `.\mvnw.cmd -version` (sous Windows, le script à invoquer est
`mvnw.cmd` et non `./mvnw`, qui est la variante Unix) :

```
PS C:\Users\bobot\Desktop\bibiotheque\bibliotheque-backend> .\mvnw.cmd -version
Apache Maven 3.8.5 (3599d3414f046de2324203b78ddcf9b5e4388aa0)
Maven home: C:\Users\bobot\.m2\wrapper\dists\apache-maven-3.8.5-bin\5i5jha092a3i37g0paqnfr15e0\apache-maven-3.8.5
Java version: 21.0.10, vendor: Oracle Corporation, runtime: C:\Program Files\Java\jdk-21.0.10
Default locale: fr_FR, platform encoding: UTF-8
OS name: "windows 11", version: "10.0", arch: "amd64", family: "windows"
```

**Réponse :**

<!-- À RÉDIGER — deux phrases, avec tes mots. Trois éléments doivent y figurer :
     1. le constat : la version affichée par Maven, comparée à celle de `java -version` ;
     2. d'où vient cette version : comment le script mvnw.cmd choisit le JDK qu'il utilise ;
     3. la variable d'environnement qui tranche, et ce qui se passerait sur une
        machine possédant plusieurs JDK.
     Tu dois pouvoir défendre cette réponse à l'oral. -->

---

### 1.3 — Échec du build

<!-- À REMPLIR — étape 2.
     Contenu attendu :
     - la sortie d'erreur complète de `.\mvnw.cmd clean package` ;
     - la cause expliquée par son mécanisme (pas une recopie du message) ;
     - les deux stratégies de résolution, avec un avantage et un inconvénient chacune. -->

---

## 2. Arborescence

<!-- À REMPLIR — étape 3. Questions 2.1 à 2.6.
     Chaque réponse doit citer le chemin exact du fichier : sans chemin, zéro point. -->

---

## 3. Docker

<!-- À REMPLIR — étape 4.
     Y consigner les choix techniques et leur justification :
     - la stratégie retenue pour l'écart entre le JDK 21 de la machine et le projet ;
     - la manière dont les paramètres de connexion sont surchargés sans toucher
       à application.properties ;
     - la manière dont le backend attend que la base soit réellement prête ;
     - le traitement de l'URL http://localhost:8080 codée en dur dans le frontend ;
     - toute modification jugée indispensable mais volontairement non faite
       (l'énoncé demande de la décrire plutôt que de la réaliser). -->

---

## 4. Trajet

### 4.1 — L'emprunt d'un livre, du clic à la base

<!-- À REMPLIR — étape 6. Tableau couche par couche.
     Au minimum : composant, service Angular, intercepteur, filtre JWT,
     contrôleur, repositories, entités, SQL final.
     Préciser ce que BorrowController.borrowBook() fait de particulier par
     rapport à la création d'un livre — regarder combien de tables sont touchées. -->

| # | Couche | Fichier (chemin exact) | Ce qui s'y passe |
|---|---|---|---|
| | | | |

### 4.2 — Trois manipulations

<!-- À REMPLIR — étape 7.
     L'une des trois ne produit aucun appel réseau : savoir laquelle et pourquoi. -->

| Manipulation | Code / comportement | Fichier responsable |
|---|---|---|
| `curl -X POST http://localhost:8080/admin/books` sans en-tête `Authorization` | | |
| Se connecter avec un compte de rôle `User`, puis ouvrir `/books` | | |
| `GET /admin/books/9999` avec un token d'administrateur valide | | |

### 4.3 — La double protection

<!-- À REMPLIR — étape 7. Cinq lignes maximum. -->

---

## 5. Bonus

<!-- À TRAITER EN DERNIER, seulement si tout le reste est fait.
     - service d'initialisation automatique (compte admin + livres) : +1
     - README de démarrage rapide en tête de PR, trois commandes maximum : +0,5
     - un vrai défaut du code existant, décrit et non corrigé : +0,5 -->
