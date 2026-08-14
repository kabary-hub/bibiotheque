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

**Nom de table visé :** `Books`, avec une majuscule. C'est écrit explicitement
dans `@Table(name = "Books")` à la ligne 9 de `Books.java`. Sans cette
annotation, Hibernate aurait déduit le nom depuis celui de la classe.

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



---

## 5. Bonus


     - service d'initialisation automatique (compte admin + livres) : +1
     - README de démarrage rapide en tête de PR, trois commandes maximum : +0,5
     - un vrai défaut du code existant, décrit et non corrigé : +0,5 -->
