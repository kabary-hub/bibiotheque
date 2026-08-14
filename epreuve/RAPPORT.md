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

#### Sortie d'erreur

```
PS C:\Users\bobot\Desktop\bibiotheque\bibliotheque-backend> .\mvnw.cmd clean package

[INFO] Scanning for projects...
[INFO] --------------------< com.ibizabroker:bibliotheque >--------------------
[INFO] Building bibliotheque 0.0.1-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] --- maven-clean-plugin:3.1.0:clean (default-clean) @ bibliotheque ---
[INFO] --- maven-resources-plugin:3.2.0:resources (default-resources) @ bibliotheque ---
[INFO] --- maven-compiler-plugin:3.8.1:compile (default-compile) @ bibliotheque ---
[INFO] Changes detected - recompiling the module!
[INFO] Compiling 22 source files to C:\Users\bobot\Desktop\bibiotheque\bibliotheque-backend\target\classes
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  29.543 s
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.8.1:compile
        (default-compile) on project bibliotheque: Fatal error compiling:
        java.lang.NoSuchFieldError: Class com.sun.tools.javac.tree.JCTree$JCImport
        does not have member field 'com.sun.tools.javac.tree.JCTree qualid' -> [Help 1]
[ERROR]
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR]
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoExecutionException
```

> **Note méthodologique.** Les deux premières tentatives ont échoué pour une
> raison sans rapport avec la question : `Could not transfer artifact
> com.google.guava:guava:jar:10.0.1 ... Connection reset`, c'est-à-dire une
> coupure réseau pendant le téléchargement des dépendances. Le build s'arrêtait
> alors à `maven-resources-plugin`, **avant d'atteindre le compilateur**. Une
> panne de téléchargement n'est pas une réponse à cette question : il a fallu
> relancer jusqu'à obtenir un échec à l'étape `compile`, seule pertinente ici.

#### Cause : le mécanisme

L'échec ne vient ni du code du projet, ni de la cible de compilation Java 8. Il
vient de **Lombok**, et plus précisément de la manière dont Lombok fonctionne.

Trois faits à mettre bout à bout :

1. **Lombok n'est pas une bibliothèque ordinaire, c'est un *annotation
   processor*.** L'annotation `@Data` sur `entity/Books.java` ne fait rien à
   l'exécution : elle demande à Lombok de **modifier l'arbre syntaxique** du
   programme *pendant* la compilation, pour y injecter les getters, setters,
   `equals()`, `hashCode()` et `toString()` qui ne sont écrits nulle part dans
   le fichier source.

2. **Pour cela, Lombok manipule les classes internes du compilateur.** Il
   accède directement à `com.sun.tools.javac.tree.JCTree`, une API **interne**
   du JDK, non publique, que les auteurs du JDK peuvent modifier à chaque
   version sans préavis — c'est justement ce que garantit *l'absence* de
   garantie de compatibilité sur les paquets `com.sun.*`.

3. **Le JDK 21 a modifié la classe `JCTree$JCImport`.** Le champ `qualid`, qui y
   existait avec le type `JCTree`, a changé. La version de Lombok présente au
   classpath — **1.18.20**, imposée par le parent `spring-boot-starter-parent`
   2.4.5, vérifiée dans `~/.m2/repository/org/projectlombok/lombok/` — a été
   compilée contre l'ancienne signature. À l'exécution, elle cherche un champ
   qui n'existe plus.

D'où le `NoSuchFieldError` : ce n'est **pas** une erreur de compilation du code
métier, c'est **le compilateur lui-même qui plante** parce qu'un de ses
greffons est incompatible avec la version du JDK qui l'exécute. Le message
`Fatal error compiling` le confirme — javac n'a pas rejeté le code, il s'est
interrompu.

En une phrase : *un projet figé en 2021 est construit par un JDK de 2023+, et
l'outil qui fait le pont entre les deux n'a jamais été mis à jour.*

À ne pas confondre avec l'avertissement `source value 8 is obsolete`, qui peut
apparaître à cause de `<java.version>1.8</java.version>` (`pom.xml`, ligne 17) :
c'est un **warning**, le JDK 21 sait toujours compiler vers Java 8. Ce n'est pas
la cause de l'échec.

#### Les deux stratégies de résolution

##### Stratégie A — Construire avec un JDK plus ancien

Ne rien changer au projet, et lui fournir l'environnement pour lequel il a été
écrit : compiler avec un JDK 11, antérieur à la rupture d'API.

| | |
|---|---|
| **Avantage** | Aucune modification du code ni du `pom.xml`. Le risque de régression est nul, et le résultat est reproductible à l'identique sur n'importe quelle machine — ce qui est exactement ce qu'un conteneur permet de garantir. Mise en œuvre immédiate. |
| **Inconvénient** | On fige le projet sur une plateforme vieillissante. Le JDK 11 et Spring Boot 2.4.5 ne reçoivent plus de correctifs de sécurité gratuits, on se prive des évolutions du langage, et la dette ne fait que grossir : le problème est repoussé, pas résolu. |

##### Stratégie B — Moderniser le projet

Relever les versions : Lombok en 1.18.30 minimum (première version compatible
JDK 21), Spring Boot en 3.x, et `<java.version>` à 17 ou 21.

| | |
|---|---|
| **Avantage** | Résout la cause plutôt que le symptôme. Le projet redevient supportable, bénéficie des correctifs de sécurité et des gains de performance du JDK récent, et peut être construit par n'importe quel développeur avec un poste à jour. |
| **Inconvénient** | Effort et risque bien supérieurs. Passer à Spring Boot 3 impose la migration `javax.*` → `jakarta.*` (toutes les entités du projet importent `javax.persistence.*`), la réécriture de `WebSecurityConfiguration` — `WebSecurityConfigurerAdapter` est supprimé — et le remplacement de la dépendance `jjwt:0.9.1`, abandonnée. Sans tests de non-régression sérieux, on casse du fonctionnel sans s'en apercevoir. |

##### Choix retenu

**Stratégie A.** Deux raisons, dans cet ordre :

1. L'énoncé de l'épreuve interdit explicitement de modifier `pom.xml`
   (partie 3, contraintes). La stratégie B est donc hors périmètre.
2. Indépendamment de cette contrainte, c'est aussi la décision que je
   défendrais en situation réelle **à ce stade** : l'objectif immédiat est de
   faire tourner l'application, et une migration Spring Boot 2 → 3 sans filet
   de tests est un chantier à part entière, qui se planifie et ne s'improvise
   pas au milieu d'une mise en conteneur.

La mise en œuvre concrète est décrite en section 3 : l'image de construction du
backend utilise `maven:3.8-eclipse-temurin-11`, ce qui règle le problème sans
qu'aucun fichier du projet ne soit touché — et sans exiger que le développeur
installe un second JDK sur son poste.

---

## 2. Arborescence

### 2.1 — Paramètres de connexion à la base

**Fichier :** `bibliotheque-backend/src/main/resources/application.properties`

Les trois paramètres y sont écrits en clair, aux lignes 3 à 5 :

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/bibliotheque
spring.datasource.username=root
spring.datasource.password=mysql
```

L'URL désigne le pilote (`jdbc:mysql`), l'hôte (`localhost`), le port (`3306`)
et le nom du schéma (`bibliotheque`). Ces trois valeurs sont celles que la
partie 3 devra surcharger par variables d'environnement, sans modifier ce
fichier.

---

### 2.2 — Ce qui rend `POST /authenticate` accessible sans être connecté

**Fichier :** `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/configuration/WebSecurityConfiguration.java`

Ligne 43, dans la méthode `configure(HttpSecurity)` :

```java
.authorizeRequests().antMatchers("/authenticate", "/borrow/**", "/admin/books/").permitAll()
.antMatchers(HttpHeaders.ALLOW).permitAll()
.anyRequest().authenticated()
```

`permitAll()` déclare la liste blanche : toute URL qui y figure échappe à
l'authentification. `anyRequest().authenticated()` applique la règle inverse à
tout le reste. L'ordre compte : la première règle qui correspond gagne.

**Observation :** cette liste blanche contient aussi `/borrow/**` et
`/admin/books/`, ce qui est beaucoup plus large que le seul `/authenticate`.
Voir la section 5 (bonus) pour l'analyse du risque.

---

### 2.3 — La classe qui devient une ligne de table MySQL

**Fichier :** `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/entity/Books.java`

La correspondance objet ↔ table est déclarée par les annotations JPA, lignes 8-9 :

```java
@Data
@Entity
@Table(name = "Books")
public class Books {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Integer bookId;
    ...
}
```

**Nom de table demandé :** `Books`, avec une majuscule. C'est écrit
explicitement dans `@Table(name = "Books")`, à la **ligne 9 de `Books.java`**.
Sans cette annotation, le nom aurait été déduit de celui de la classe.

**Mais ce n'est pas le nom réellement créé en base**, et c'est un piège
classique. Spring Boot installe par défaut une stratégie de nommage physique,
`CamelCaseToUnderscoresNamingStrategy`, qui s'applique **y compris aux noms
écrits à la main dans `@Table`** : elle les met en minuscules et convertit le
camelCase en `snake_case`. La table effectivement créée s'appelle donc `books`,
et les colonnes `book_id`, `book_name`, `book_author`, `no_of_copies`.

Deux éléments corroborent cette lecture : les instructions SQL du README pour
créer le premier compte manipulent bien `users`, `role` et `user_role` en
minuscules ; et le README précise lui-même que les noms sont convertis en
`snake_case`, en invitant à les vérifier plutôt qu'à les supposer.

Sur MySQL sous Linux — donc dans le conteneur — les noms de tables sont
sensibles à la casse. Écrire `SELECT * FROM Books;` échouerait ; il faut
`SELECT * FROM books;`. Sous Windows, la même requête passerait, la casse y
étant ignorée par défaut : une différence de comportement entre le poste de
développement et le conteneur.

Nuance importante : la classe `Books` ne fait que **déclarer** la
correspondance. La transformation réelle en `INSERT` / `SELECT` est effectuée à
l'exécution par Hibernate, l'implémentation de JPA fournie par
`spring-boot-starter-data-jpa`.

---

### 2.4 — Qui écrit le corps de `save()`

**Fichier :** `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/dao/BooksRepository.java`

```java
@Repository
public interface BooksRepository extends JpaRepository<Books, Integer> {
}
```

L'interface est vide, et pourtant `save()`, `findAll()`, `findById()` ou
`deleteById()` sont utilisables. Personne n'écrit ce code à la main :

* **Qui ?** Spring Data JPA. Il génère une classe d'implémentation dynamique
  (un proxy) qui délègue à `SimpleJpaRepository`, la classe fournie par le
  framework et qui contient le vrai corps de `save()`.
* **Quand ?** **Au démarrage de l'application**, pas à la compilation. Pendant
  la création du contexte Spring, le scan repère les interfaces qui étendent
  `JpaRepository`, et pour chacune un bean proxy est instancié et injecté.

C'est vérifiable : le dossier `target/classes` ne contient aucun fichier
`BooksRepositoryImpl.class`. L'implémentation n'existe qu'en mémoire, à
l'exécution.

---

### 2.5 — L'adresse `http://localhost:8080` codée en dur

Quatre occurrences réparties dans trois fichiers, tous situés dans
`bibliotheque-frontend/src/app/_service/` :

| # | Fichier (chemin exact) | Ligne | Occurrence |
|---|---|---|---|
| 1 | `bibliotheque-frontend/src/app/_service/users.service.ts` | 13 | `private baseURL = "http://localhost:8080/admin/users";` |
| 2 | `bibliotheque-frontend/src/app/_service/users.service.ts` | 24 | `this.httpClient.post("http://localhost:8080/authenticate", ...)` |
| 3 | `bibliotheque-frontend/src/app/_service/borrow.service.ts` | 11 | `private baseURL = "http://localhost:8080/borrow";` |
| 4 | `bibliotheque-frontend/src/app/_service/books.service.ts` | 11 | `private baseURL = "http://localhost:8080/admin/books";` |

#### Pourquoi c'est un problème dès qu'on déploie ailleurs

Ce code TypeScript est compilé en JavaScript et **exécuté par le navigateur du
visiteur**, jamais par le serveur qui l'héberge. `localhost` n'est donc pas
résolu depuis la machine de déploiement, mais **depuis le poste de
l'utilisateur**. Trois conséquences :

1. **En production**, le navigateur d'un visiteur tenterait de joindre *son
   propre* port 8080. Il n'y a rien qui écoute dessus : toutes les requêtes
   échouent, sans qu'aucune erreur n'apparaisse dans les journaux du serveur.
2. **En conteneur**, le même raisonnement s'applique : mettre `backend:8080`
   (le nom de service Docker) ne marcherait pas davantage, car ce nom n'est
   résolvable que depuis l'intérieur du réseau Docker, pas depuis le navigateur.
3. **La valeur est figée à la compilation.** Changer d'environnement impose de
   reconstruire le bundle, alors qu'un déploiement devrait pouvoir se
   reconfigurer sans recompilation.

#### La solution prévue par Angular, déjà présente mais inutilisée

Le projet contient `bibliotheque-frontend/src/environments/environment.ts` et
`environment.prod.ts`. C'est précisément le mécanisme prévu pour cela : y
déclarer une `apiUrl`, l'importer dans les trois services, et laisser le
remplacement de fichier configuré dans `angular.json` substituer la valeur de
production au moment du build. Aucun des trois services n'utilise ce mécanisme.

---

### 2.6 — Routes et rôles

**Fichier :** `bibliotheque-frontend/src/app/app-routing.module.ts`

#### Routes réservées au rôle `Admin` (8)

| Route | Composant | Ligne |
|---|---|---|
| `books` | `BooksListComponent` | 19 |
| `create-book` | `CreateBookComponent` | 20 |
| `update-book/:bookId` | `UpdateBookComponent` | 22 |
| `book-details/:bookId` | `BookDetailsComponent` | 23 |
| `users` | `UsersListComponent` | 24 |
| `register-user` | `RegistrationComponent` | 25 |
| `user-details/:userId` | `UserDetailsComponent` | 26 |
| `update-user/:userId` | `UpdateUserComponent` | 27 |

#### Routes réservées au rôle `User` (2)

| Route | Composant | Ligne |
|---|---|---|
| `borrow-book` | `BorrowBookComponent` | 30 |
| `return-book` | `ReturnBookComponent` | 31 |

Toutes portent `canActivate:[AuthGuard]` accompagné de
`data:{roles:['Admin']}` ou `data:{roles:['User']}`. Le garde lit ce tableau
`roles` (`auth.guard.ts`, ligne 22) et le compare au rôle stocké côté client.

#### Les trois routes sans `canActivate`

| Route | Composant | Ligne | Est-ce normal ? |
|---|---|---|---|
| `''` (racine) | `HomeComponent` | 21 | **Oui.** C'est la page d'accueil, premier point d'entrée d'un visiteur anonyme. Son contenu se réduit d'ailleurs à un titre de bienvenue, sans aucune donnée sensible. |
| `login` | `LoginComponent` | 28 | **Oui, et c'est indispensable.** La protéger créerait une boucle infinie : le garde redirige les non-connectés vers `/login`, qui les redirigerait à nouveau vers `/login`. |
| `forbidden` | `ForbiddenComponent` | 29 | **Oui, pour la même raison.** C'est la destination vers laquelle le garde renvoie en cas de rôle insuffisant (`auth.guard.ts`, ligne 30). La protéger rendrait le message d'erreur lui-même inatteignable. |

Les trois exceptions sont donc justifiées. Il faut néanmoins souligner que
l'absence de garde ne rend pas ces pages dangereuses : la protection réelle des
**données** est assurée côté serveur (voir 4.3), pas par le routeur Angular.

**Remarque annexe :** il n'existe aucune route générique `**`. Une URL inconnue
n'affiche donc ni page 404 ni redirection, mais un gabarit vide.

---

## 3. Docker

### Fichiers ajoutés

| Fichier | Rôle |
|---|---|
| `docker-compose.yml` | Orchestration des trois services `db`, `backend`, `frontend` |
| `bibliotheque-backend/Dockerfile` | Construction en deux étapes puis exécution de l'API |
| `bibliotheque-backend/.dockerignore` | Exclut `target/` et le wrapper Maven du contexte de build |
| `bibliotheque-frontend/Dockerfile` | Construction Angular puis service des fichiers statiques |
| `bibliotheque-frontend/nginx.conf` | Repli SPA, cache et compression |
| `bibliotheque-frontend/.dockerignore` | Exclut `node_modules/` et `dist/` du contexte de build |
| `.env.example` | Modèle de configuration ; `.env` est ignoré par git |

Aucun fichier existant du projet n'a été modifié, conformément aux contraintes :
ni `pom.xml`, ni `package.json`, ni `application.properties`.

---

### 3.1 — L'écart de JDK

**Problème.** Le poste dispose d'un JDK 21 ; le projet ne compile qu'avec un JDK
antérieur (voir 1.3).

**Solution retenue.** L'étape de construction du backend part de
`maven:3.8-eclipse-temurin-11`. Le JDK 11 est antérieur à la modification de
`JCTree$JCImport` qui met Lombok 1.18.20 en échec.

C'est ici que la conteneurisation prend tout son sens : la version du JDK
utilisée pour construire devient une **propriété du projet**, inscrite dans le
`Dockerfile` et versionnée, au lieu d'être une propriété du poste de chaque
développeur. Personne n'a besoin d'installer un second JDK, et le résultat est
identique partout.

### 3.2 — Surcharger la configuration sans toucher `application.properties`

**Problème.** `application.properties` pointe sur `localhost:3306`, ce qui, dans
un conteneur, désigne le conteneur backend lui-même — où rien n'écoute. Le
fichier ne doit pas être modifié.

**Solution.** Spring Boot applique un **ordre de priorité** entre ses sources de
configuration : les variables d'environnement priment sur le fichier
`.properties` embarqué dans le jar. La correspondance se fait par *relaxed
binding* — `spring.datasource.url` se lit `SPRING_DATASOURCE_URL` (majuscules,
points remplacés par des soulignements).

Trois variables suffisent, déclarées dans `docker-compose.yml` :

```yaml
SPRING_DATASOURCE_URL: jdbc:mysql://db:3306/bibliotheque?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME: root
SPRING_DATASOURCE_PASSWORD: ${MYSQL_ROOT_PASSWORD:-mysql}
```

Deux points méritent d'être justifiés :

* **`db` et non `localhost`.** `db` est le nom du service dans le compose ; le
  DNS interne du réseau Docker le résout en l'adresse du conteneur MySQL.
* **`allowPublicKeyRetrieval=true&useSSL=false`.** MySQL 8 authentifie par
  défaut avec `caching_sha2_password`, qui exige soit une connexion TLS, soit
  l'autorisation explicite de récupérer la clé publique du serveur. Sans ces
  paramètres, la connexion échoue alors même que l'hôte et le mot de passe sont
  corrects — une erreur difficile à diagnostiquer.

**Aucun mot de passe n'apparaît dans les `Dockerfile`** (critère 3.5). Les
valeurs transitent par variables d'environnement, avec la syntaxe
`${VARIABLE:-défaut}` : le projet démarre sans configuration préalable sur une
machine vierge, tout en restant surchargeable par un `.env` local, non versionné.

### 3.3 — Attendre que la base soit *réellement* prête

**Problème.** `depends_on` seul n'attend que le **démarrage du conteneur**, pas
la **disponibilité du serveur**. Au premier lancement, MySQL initialise ses
fichiers de données pendant plusieurs dizaines de secondes. Le backend
démarrerait pendant ce temps, échouerait à se connecter, et Spring Boot
s'arrêterait — le conteneur mourrait au premier `up`.

**Solution en deux parties**, les deux étant nécessaires :

```yaml
# sur le service db
healthcheck:
  test: ["CMD-SHELL", "mysqladmin ping -h 127.0.0.1 -u root -p$$MYSQL_ROOT_PASSWORD --silent"]
  interval: 5s
  retries: 30
  start_period: 40s

# sur le service backend
depends_on:
  db:
    condition: service_healthy
```

`mysqladmin ping` interroge réellement le serveur ; le conteneur n'est déclaré
`healthy` que lorsqu'il répond. `condition: service_healthy` — et non
`service_started` — est ce qui fait attendre le backend.

Le `$$` échappe le `$` : sans lui, Compose tenterait de substituer la variable
lui-même au lieu de la transmettre au shell du conteneur.

### 3.4 — La persistance des données

Le volume nommé `db-data`, monté sur `/var/lib/mysql`, est ce qui fait survivre
les données à un `docker compose down` suivi d'un `up`. Sans lui, les écritures
resteraient dans la couche inscriptible du conteneur, détruite avec lui.

### 3.5 — Le piège de `http://localhost:8080`

**Le raisonnement.** Ce code est exécuté par le **navigateur** de l'utilisateur,
pas par le conteneur `frontend`. `localhost` y désigne donc la machine de
l'utilisateur, c'est-à-dire l'hôte Docker.

Deux fausses solutions, à écarter explicitement :

* Remplacer l'adresse par `backend:8080` — le nom de service n'est résolvable
  que **depuis l'intérieur** du réseau Docker, auquel le navigateur
  n'appartient pas.
* Faire relayer les appels par nginx — le frontend émet des URL **absolues**
  vers `localhost:8080`, qui ne passent jamais par nginx. Un relais n'aurait
  d'effet que si les URL étaient relatives.

**Solution retenue.** Publier le port du backend sur l'hôte
(`ports: - "8080:8080"`). Le `localhost:8080` du navigateur atteint alors le
port publié, donc le conteneur. C'est la seule solution qui ne modifie aucun
fichier source du frontend.

La configuration CORS étant permissive (`CorsConfiguration.java`, ligne 24 :
`allowedOriginPatterns("*")`), l'origine `http://localhost:4200` est acceptée
sans réglage supplémentaire.

### 3.6 — Modification jugée nécessaire, volontairement non faite

L'énoncé demande de décrire plutôt que de réaliser. La voici.

**Ce qu'il faudrait faire.** Remplacer les quatre occurrences de
`http://localhost:8080` (voir 2.5) par une valeur lue depuis
`src/environments/environment.ts`, en y déclarant `apiUrl: ''` pour la
production. Les URL deviendraient relatives (`/admin/books`), donc servies par
la même origine que l'interface, et `nginx.conf` relaierait `/admin`,
`/authenticate` et `/borrow` vers `http://backend:8080` via `proxy_pass`.

**Pourquoi c'est supérieur.** Trois gains : plus besoin de publier le port 8080
sur l'hôte, donc l'API cesse d'être exposée directement ; plus de requêtes
cross-origin, donc la configuration CORS permissive devient inutile ; et le
déploiement sur un autre domaine fonctionne sans recompiler.

**Pourquoi ce n'est pas fait ici.** La solution retenue en 3.5 suffit à l'usage
local demandé, et cette modification touche au code source du frontend —
au-delà du périmètre de l'exercice, qui porte sur la mise en conteneur.

### 3.7 — Le service `seed` (bonus)

**Problème.** Sur une machine vierge, `docker compose up` donnait une
application dans laquelle il était impossible d'entrer : la base est vide,
aucun compte n'existe, et `POST /admin/users` exige déjà un jeton. Il fallait
insérer le premier administrateur en SQL à la main (README, section 5).

**Pourquoi `/docker-entrypoint-initdb.d` ne convient pas.** C'est le mécanisme
habituel de l'image MySQL, mais il s'exécute **avant** que les tables
n'existent : ce sont Hibernate et `ddl-auto=update` qui les créent, au démarrage
du backend, donc bien après l'initialisation de MySQL. Un script placé là
échouerait sur des tables inconnues.

**Solution.** Un quatrième service, `seed`, qui réutilise l'image `mysql:8.0`
déjà téléchargée — pour son client `mysql`, aucune image supplémentaire n'est
nécessaire. Il démarre après le backend, interroge la base jusqu'à ce que la
table `books` soit interrogeable, puis joue `docker/seed.sql`, et s'arrête.

Son état final `Exited (0)` dans `docker compose ps -a` est **le comportement
attendu**, pas une panne : c'est une tâche ponctuelle, d'où `restart: "no"`.

Le script insère deux comptes (`admin` / `lecteur`, mot de passe `admin123`,
haché en BCrypt) et six livres, dont un à exemplaire unique pour pouvoir tester
le message de rupture de stock. Toutes les insertions sont en `INSERT IGNORE` :
le service peut être rejoué sans créer de doublon.

Il avance enfin le compteur de `hibernate_sequence` au-delà des identifiants
posés à la main. Sans cela, la première création de livre depuis l'interface
entrerait en collision avec une clé déjà prise.

**Un fichier `.gitattributes` accompagne ce service** avec la règle
`*.sh text eol=lf`. Sans elle, git extrait le script avec des fins de ligne
CRLF sur un poste Windows ; le noyau Linux lit alors `#!/bin/bash\r`, ne trouve
aucun interpréteur de ce nom, et le conteneur échoue sur un `exec format error`
dont la cause est très difficile à voir.

### 3.8 — Autres décisions

* **Images en deux étapes.** L'image finale du backend ne contient ni Maven, ni
  les sources, ni le cache de dépendances : seulement un JRE et le jar. Même
  logique côté frontend, où Node disparaît au profit de nginx seul.
* **Tests ignorés à la construction** (`-DskipTests`).
  `BibliothequeApplicationTests` charge le contexte Spring complet et exige donc
  une base MySQL joignable. Elle ne l'est pas pendant un `docker build`, qui ne
  participe pas au réseau de compose.
* **Utilisateur non privilégié.** Le backend s'exécute sous l'utilisateur
  `spring`, pas sous `root`.
* **Repli SPA nginx.** Sans le `try_files` de `nginx.conf`, recharger la page
  sur `/books` renverrait un 404 : le routage est assuré par Angular dans le
  navigateur, pas par le serveur.

---

## 4. Trajet

### 4.1 — L'emprunt d'un livre, du clic à la base

Scénario : un utilisateur de rôle `User` est sur `/borrow-book` et clique sur le
bouton **Borrow** de la ligne d'un livre.

#### Le trajet, couche par couche

| # | Couche | Fichier (chemin exact) | Ce qui s'y passe |
|---|---|---|---|
| 1 | Gabarit HTML | `bibliotheque-frontend/src/app/borrow-book/borrow-book.component.html` (ligne 21) | `<button (click)="borrowBook(book.bookId)">` — le clic transmet l'identifiant du livre de la ligne courante. |
| 2 | Composant Angular | `bibliotheque-frontend/src/app/borrow-book/borrow-book.component.ts` (lignes 37-45) | Construit l'objet `Borrow` : `bookId` vient du clic, `userId` de `userAuthService.getUserId()`, c'est-à-dire du `localStorage` du navigateur. Puis appelle le service. |
| 3 | Service Angular | `bibliotheque-frontend/src/app/_service/borrow.service.ts` (lignes 11, 19-21) | `httpClient.post("http://localhost:8080/borrow", borrow)` — l'objet est sérialisé en JSON. Rien n'est encore parti : `HttpClient` renvoie un `Observable` froid, la requête n'est émise qu'au `.subscribe()` de l'étape 2. |
| 4 | Enregistrement de l'intercepteur | `bibliotheque-frontend/src/app/app.module.ts` (lignes 56-60) | `{ provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }`. C'est **ici** que se joue l'énigme de l'en-tête `Authorization` : il est ajouté globalement, une fois, et aucun composant n'a à s'en soucier. |
| 5 | Intercepteur HTTP | `bibliotheque-frontend/src/app/_auth/auth.interceptor.ts` (lignes 15-22, 39-47) | Clone la requête et y greffe `Authorization: Bearer <token>`. Une requête `HttpRequest` étant immuable, on ne peut pas la modifier : d'où le `request.clone({ setHeaders: ... })`. |
| | | **— frontière réseau —** | La requête quitte le navigateur : `POST http://localhost:8080/borrow`, corps `{"bookId":1,"userId":2}`, en-tête `Authorization`. |
| 6 | Filtre CORS | `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/configuration/CorsConfiguration.java` (lignes 21-25) | L'origine du navigateur (`http://localhost:4200`) diffère de celle de l'API (`:8080`). Le navigateur émet d'abord un `OPTIONS` de contrôle préalable ; c'est cette configuration qui l'autorise. |
| 7 | Chaîne de sécurité | `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/configuration/WebSecurityConfiguration.java` (ligne 43) | `antMatchers("/authenticate", "/borrow/**", "/admin/books/").permitAll()`. **`/borrow/**` figure dans la liste blanche** : cette URL est publique. Le jeton envoyé à l'étape 5 n'est donc pas exigé (voir section 5). |
| 8 | Filtre JWT | `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/configuration/JwtRequestFilter.java` (lignes 30-61) | `OncePerRequestFilter` : s'exécute pour **toute** requête, y compris publique. Lit l'en-tête, retire le préfixe `Bearer `, extrait le nom d'utilisateur, recharge le `UserDetails`, valide la signature et l'expiration, puis pose l'authentification dans le `SecurityContextHolder`. |
| 9 | Contrôleur | `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/controller/BorrowController.java` (lignes 31-53) | `@PostMapping` sur `@RequestMapping("/borrow")`. Jackson désérialise le corps JSON en objet `Borrow` via `@RequestBody`. |
| 10 | Repository — lecture 1 | `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/dao/UsersRepository.java` (appelé ligne 33 du contrôleur) | `findById(borrow.getUserId())` → `SELECT` sur la table `Users`, uniquement pour composer le message de retour. |
| 11 | Repository — lecture 2 | `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/dao/BooksRepository.java` (appelé ligne 34) | `findById(borrow.getBookId())` → `SELECT` sur la table `Books`. |
| 12 | Règle métier | `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/controller/BorrowController.java` (lignes 36-38) | `if (book.getNoOfCopies() < 1)` : sortie anticipée avec un message texte si le stock est épuisé. |
| 13 | Entité — logique | `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/entity/Books.java` (lignes 20-22) | `book.borrowBook()` décrémente `noOfCopies` **en mémoire**. Rien n'est encore écrit. |
| 14 | Écriture 1 | `BooksRepository.save(book)` (ligne 41 du contrôleur) | L'entité ayant déjà un identifiant, Hibernate émet un `UPDATE` sur `Books`. |
| 15 | Dates | `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/controller/BorrowController.java` (lignes 43-50) | `issueDate` = maintenant ; `dueDate` = maintenant + 7 jours, via `Calendar.add(Calendar.DATE, 7)`. Ces deux champs ne viennent pas du client. |
| 16 | Entité — correspondance | `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/entity/Borrow.java` (lignes 10-32) | `@Entity @Table(name = "Borrow")`, clé `borrowId` en `GenerationType.IDENTITY` (auto-incrément MySQL). |
| 17 | Écriture 2 | `BorrowRepository.save(borrow)` (ligne 51 du contrôleur) | `borrowId` étant nul, Hibernate émet un `INSERT` dans `Borrow`. |
| 18 | Réponse | `BorrowController.borrowBook()` (ligne 52) | Retourne une **chaîne de caractères**, pas un objet : `"Alice has borrowed one copy of \"Dune\"!"`. |
| 19 | Retour navigateur | `bibliotheque-frontend/src/app/borrow-book/borrow-book.component.ts` (lignes 41-44) | Le `.subscribe()` reçoit la réponse et se contente d'un `console.log`. Aucun rafraîchissement de la liste : le compteur de copies affiché reste périmé jusqu'au rechargement de la page. |

#### Le SQL effectivement produit

Avec `spring.jpa.show-sql=true` (`application.properties`, ligne 7), la console
du backend affiche quatre instructions, dans cet ordre :

```sql
-- 10. lecture de l'emprunteur
select users0_.user_id as user_id1_2_0_, ... from users users0_ where users0_.user_id=?

-- 11. lecture du livre
select books0_.book_id as book_id1_0_0_, ... from books books0_ where books0_.book_id=?

-- 14. decrement du stock
update books set book_author=?, book_genre=?, book_name=?, no_of_copies=? where book_id=?

-- 17. creation de la ligne d'emprunt
insert into borrow (book_id, due_date, issue_date, return_date, user_id) values (?, ?, ?, ?, ?)
```

Deux observations sur ces noms :

* Les colonnes sont en `snake_case` alors que les champs Java sont en
  `camelCase` (`bookId` → `book_id`).
* Les tables sont en **minuscules**, alors que les annotations écrivent
  `@Table(name = "Books")` et `@Table(name = "Borrow")` avec une majuscule.

Les deux conversions sont l'œuvre de la même stratégie de nommage physique de
Spring Boot, `CamelCaseToUnderscoresNamingStrategy`, détaillée en 2.3.

#### Ce que `borrowBook()` fait de particulier

**La création d'un livre touche une seule table. L'emprunt en touche trois.**

| | Création d'un livre (`POST /admin/books`) | Emprunt (`POST /borrow`) |
|---|---|---|
| Tables lues | aucune | `Users`, `Books` |
| Tables écrites | `Books` (1 `INSERT`) | `Books` (1 `UPDATE`) **et** `Borrow` (1 `INSERT`) |
| Repositories mobilisés | 1 | 3 |
| Instructions SQL | 1 | 4 |

C'est le passage d'une opération **atomique par nature** à une opération
**composite** : deux écritures qui n'ont de sens que réalisées ensemble. Un
emprunt enregistré sans décrément fausse le stock ; un décrément sans emprunt
enregistré fait disparaître une copie sans trace.

Or — et c'est le point à souligner — **la méthode ne porte aucune annotation
`@Transactional`**. Chaque `save()` s'exécute donc dans sa propre transaction,
ouverte et validée indépendamment par `SimpleJpaRepository`. Conséquence
concrète : si l'`INSERT` de la ligne 51 échoue (contrainte violée, coupure de
connexion), l'`UPDATE` de la ligne 41 est **déjà validé et ne sera pas annulé**.
Le stock a diminué, aucun emprunt n'existe, et rien ne le signale.

Deux fragilités du même ordre, visibles dans les mêmes lignes :

* **Lignes 33-34** : `findById(...).get()` sans `orElseThrow()`. Un identifiant
  inexistant lève `NoSuchElementException`, qui remonte en **HTTP 500** au lieu
  du 404 attendu — alors que le projet définit pourtant une
  `NotFoundException` (`exceptions/NotFoundException.java`), utilisée ailleurs.
* **Ligne 36** : le contrôle de stock renvoie un message dans une réponse
  **HTTP 200**. Côté Angular, le `.subscribe()` traite donc un échec métier
  comme un succès : l'utilisateur ne voit aucune erreur.

### 4.2 — Trois manipulations

| Manipulation | Code / comportement | Fichier responsable |
|---|---|---|
| `curl -X POST http://localhost:8080/admin/books` sans en-tête `Authorization` | **401 Unauthorized** | `bibliotheque-backend/.../configuration/JwtAuthenticationEntryPoint.java` (ligne 17) |
| Se connecter avec un compte de rôle `User`, puis ouvrir `/books` | **Aucun appel réseau.** Redirection immédiate vers `/forbidden` | `bibliotheque-frontend/src/app/_auth/auth.guard.ts` (lignes 24-32) |
| `GET /admin/books/9999` avec un token d'administrateur valide | **404 Not Found** | `bibliotheque-backend/.../controller/BooksController.java` (ligne 31) + `.../exceptions/NotFoundException.java` (ligne 6) |

#### Détail de chaque cas

**1 — `POST /admin/books` sans jeton → 401**

`JwtRequestFilter` s'exécute, ne trouve aucun en-tête `Authorization`, affiche
`JWT token does not start with Bearer` et laisse le `SecurityContext` vide. La
requête arrive ensuite à la règle `anyRequest().authenticated()`
(`WebSecurityConfiguration.java`, ligne 45), qui la rejette. Spring Security
délègue alors au point d'entrée configuré ligne 47, dont l'unique instruction
est `response.sendError(SC_UNAUTHORIZED, "Unauthorized")`.

*Subtilité à ne pas manquer :* la liste blanche de la ligne 43 contient
`"/admin/books/"` — **avec une barre oblique finale**. Le motif ne correspond
pas à `/admin/books`, que curl appelle sans barre finale. C'est ce détail d'un
seul caractère qui fait que la requête est bien rejetée. Le `@PreAuthorize` de
`BooksController` (ligne 35) n'est jamais atteint : le filtre de sécurité
intervient avant que le contrôleur ne soit invoqué.

**2 — Rôle `User` sur `/books` → aucun appel réseau**

**C'est la manipulation qui ne produit aucune requête HTTP**, et il faut savoir
dire pourquoi : tout se joue **avant** l'existence du composant.

Le routeur Angular exécute `AuthGuard.canActivate()` avant d'activer la route.
Le jeton existe (l'utilisateur est connecté), le garde lit alors
`route.data["roles"]`, qui vaut `['Admin']` pour la route `books`
(`app-routing.module.ts`, ligne 19). `roleMatch(['Admin'])` renvoie `false`, le
garde appelle `this.router.navigate(['/forbidden'])` et retourne `false`.

La route n'étant jamais activée, `BooksListComponent` n'est **jamais
instancié** ; son `ngOnInit()` — qui contient l'appel à
`booksService.getBooksList()` — n'est donc jamais exécuté. L'onglet *Réseau*
des DevTools reste vide. Tout se passe en mémoire, dans le navigateur.

**3 — `GET /admin/books/9999` avec un jeton admin → 404**

Le trajet va cette fois jusqu'au bout. `JwtRequestFilter` valide le jeton et
place l'authentification dans le contexte, avec l'autorité `ROLE_Admin` —
construite par `JwtService.getAuthority()` (ligne 64), qui préfixe le nom du
rôle stocké en base par `ROLE_`. C'est ce préfixe qui fait que
`@PreAuthorize("hasRole('Admin'))"` (`BooksController.java`, ligne 28) est
satisfait : `hasRole` ajoute implicitement `ROLE_` à son argument.

Le contrôleur s'exécute donc, `findById(9999)` renvoie un `Optional` vide, et
`orElseThrow()` lève `NotFoundException`. Cette classe portant
`@ResponseStatus(value = HttpStatus.NOT_FOUND)` (`NotFoundException.java`,
ligne 6), Spring MVC traduit l'exception en **404** au lieu du 500 par défaut.

À rapprocher de ce qui a été relevé en 4.1 : `BorrowController` utilise
`.get()` au lieu de `orElseThrow()` et renvoie donc, lui, un 500 dans la même
situation. Deux contrôleurs du même projet, deux comportements différents.

### 4.3 — La double protection

Non, elle n'est pas redondante : les deux protections n'ont **pas le même
objet**. `auth.guard.ts` sert le confort d'usage — éviter d'afficher une page
inutilisable — tandis que `@PreAuthorize` seul protège réellement les
**données**. S'il ne fallait en garder qu'une, ce serait **`@PreAuthorize`** :
le navigateur est entièrement sous le contrôle de l'utilisateur, qui peut
modifier son `localStorage` ou appeler l'API directement en curl, sans jamais
exécuter le garde. Le serveur, lui, ne peut pas être contourné.

> **Nuance à signaler sur cet exemple précis.** L'énoncé postule que la liste
> des livres est protégée par `@PreAuthorize` côté serveur. Ce n'est pas le
> cas : `BooksController.getAllBooks()` (`BooksController.java`, lignes 23-26)
> est la **seule** méthode de la classe qui n'en porte pas. `GET /admin/books`
> n'exige donc qu'une authentification, sans contrôle de rôle. Un compte de
> rôle `User` à qui l'interface interdit `/books` peut malgré tout obtenir la
> liste complète en appelant l'API avec son propre jeton. C'est précisément la
> démonstration du raisonnement ci-dessus : ici, la protection côté navigateur
> existe, la protection côté serveur manque, et c'est celle qui manque qui
> compte.



---

## 5. Bonus — un défaut du code existant

**`POST /borrow` est une route publique qui accepte un `userId` fourni par le
client. N'importe qui, sans compte, peut emprunter un livre au nom de
n'importe quel utilisateur.**

### Le défaut

Deux décisions, anodines prises séparément, ouvrent la faille une fois
combinées.

**1. La route est en accès libre.**
`bibliotheque-backend/.../configuration/WebSecurityConfiguration.java`, ligne 43 :

```java
.antMatchers("/authenticate", "/borrow/**", "/admin/books/").permitAll()
```

`/borrow/**` couvre l'intégralité du contrôleur d'emprunt : le `POST` qui
emprunte, le `PUT` qui restitue, et les `GET` qui listent l'historique. Aucun
jeton n'est exigé. On peut supposer que la ligne visait à débloquer un problème
de développement, mais elle est restée.

**2. Le serveur fait confiance à l'identité envoyée par le client.**
`bibliotheque-backend/.../controller/BorrowController.java`, ligne 33 :

```java
Users user = usersRepository.findById(borrow.getUserId()).get();
```

`borrow.getUserId()` provient du **corps JSON de la requête**. Le champ est
rempli côté navigateur depuis le `localStorage`
(`borrow-book.component.ts`, ligne 39), mais rien ne l'impose : le serveur
accepte la valeur telle quelle.

L'information fiable existe pourtant. `JwtRequestFilter` a placé
l'utilisateur authentifié dans le `SecurityContextHolder` (ligne 58) — le
contrôleur ne la consulte jamais.

### Le risque

Une seule commande, sans aucun compte :

```bash
curl -X POST http://localhost:8080/borrow \
     -H "Content-Type: application/json" \
     -d '{"bookId":1,"userId":2}'
```

Conséquences, par gravité croissante :

1. **Usurpation.** Des emprunts sont enregistrés au nom d'un tiers, qui en
   devient comptable. La table `Borrow` — le registre qui fait foi — n'est plus
   digne de confiance.
2. **Corruption du stock.** Chaque appel décrémente `noOfCopies`
   (`Books.java`, ligne 21). Une boucle de quelques secondes met tout le
   catalogue à zéro. Le contrôle `< 1` de la ligne 36 empêche de passer sous
   zéro, mais rien ne limite la cadence ni ne vérifie que l'emprunteur existe.
3. **Aucune traçabilité.** La route étant anonyme, les journaux ne conservent
   aucune identité exploitable. L'attaque est indétectable *a posteriori*.

Le tout est exposé publiquement dès que le port 8080 est joignable.

### La correction proposée

Trois modifications, de la plus urgente à la plus structurelle.

**1. Retirer `/borrow/**` de la liste blanche.** Un caractère à supprimer.
Seul `/authenticate` a une raison légitime d'y figurer : c'est la route qui
délivre le jeton, on ne peut pas exiger un jeton pour l'obtenir.

**2. Déduire l'emprunteur du jeton, jamais du corps de la requête.**

```java
@PostMapping
public String borrowBook(@RequestBody Borrow borrow, Principal principal) {
    Users user = usersRepository.findByUsername(principal.getName())
        .orElseThrow(() -> new NotFoundException("Utilisateur inconnu."));
    borrow.setUserId(user.getUserId());   // on écrase ce qu'a envoyé le client
    ...
}
```

Le principe général : **une donnée d'identité ne doit jamais provenir de
l'extérieur** quand le serveur la détient déjà de façon fiable.

**3. Rendre la méthode transactionnelle.** Un `@Transactional` sur
`borrowBook()` — voir 4.1 : sans lui, le décrément du stock et l'enregistrement
de l'emprunt sont validés séparément, et un échec du second laisse le premier
en base.

### Pourquoi ce défaut plutôt qu'un autre

Trois autres faiblesses ont été relevées au fil du rapport : la configuration
CORS permissive (`allowedOriginPatterns("*")` avec `allowCredentials(true)`),
l'absence de `@PreAuthorize` sur `getAllBooks()` (voir 4.3), et les `.get()`
sur `Optional` qui produisent des 500 au lieu de 404 (voir 4.1).

Celui-ci les dépasse parce qu'il est **exploitable sans authentification, en
une seule requête, et qu'il altère durablement les données**. Les autres
exigent un compte valide ou ne dégradent que la qualité des réponses.
