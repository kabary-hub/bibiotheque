-- =============================================================================
--  Jeu de donnees initial
-- =============================================================================
--  Joue une seule fois, au premier demarrage, par le service « seed » de
--  docker-compose.yml. Sans lui, l'application demarre sur une base vide :
--  aucun compte n'existe, et POST /admin/users exige deja un jeton — on ne
--  peut donc pas creer le premier administrateur par l'API.
--
--  Ce script s'execute APRES le backend, et non avant : c'est Hibernate qui
--  cree les tables au demarrage (spring.jpa.hibernate.ddl-auto=update). Un
--  script place dans /docker-entrypoint-initdb.d de MySQL s'executerait trop
--  tot, sur une base sans tables.
--
--  Noms de tables en minuscules : la strategie de nommage de Spring Boot
--  convertit @Table(name = "Books") en table « books ». Voir 2.3 du rapport.
--
--  Toutes les insertions sont en INSERT IGNORE : le script peut etre rejoue
--  sans creer de doublon ni echouer.
-- =============================================================================


-- --- Roles -------------------------------------------------------------------
-- roleId est en GenerationType.IDENTITY : les identifiants explicites sont
-- acceptes sans perturber l'auto-increment.

INSERT IGNORE INTO role (role_id, role_name) VALUES
    (1, 'Admin'),
    (2, 'User');


-- --- Comptes -----------------------------------------------------------------
-- Les mots de passe sont des hachages BCrypt : WebSecurityConfiguration
-- declare un BCryptPasswordEncoder, un mot de passe en clair serait rejete.
-- Le hachage ci-dessous correspond a « admin123 » (source : README, section 5).

INSERT IGNORE INTO users (user_id, username, name, password) VALUES
    (1, 'admin',   'Administrateur',
        '$2b$10$RN5ij7XXjDpRBALhITW.2uzYGontX4U9c9ZRH5i3e.5l6RvkjZ696'),
    (2, 'lecteur', 'Lecteur de demonstration',
        '$2b$10$RN5ij7XXjDpRBALhITW.2uzYGontX4U9c9ZRH5i3e.5l6RvkjZ696');


-- --- Association compte / role ----------------------------------------------
-- Table nommee USER_ROLE dans @JoinTable (Users.java, ligne 19), convertie en
-- « user_role » par la meme strategie de nommage.

INSERT IGNORE INTO user_role (user_id, role_id) VALUES
    (1, 1),   -- admin   -> Admin
    (2, 2);   -- lecteur -> User


-- --- Catalogue ---------------------------------------------------------------
-- De quoi voir immediatement une liste peuplee, et disposer d'un cas limite :
-- « Fondation » n'a qu'un seul exemplaire, ce qui permet de tester le message
-- « out of stock » de BorrowController apres un premier emprunt.

INSERT IGNORE INTO books (book_id, book_name, book_author, book_genre, no_of_copies) VALUES
    (1, 'Dune',                        'Frank Herbert',      'Science-fiction', 4),
    (2, 'Le Nom de la rose',           'Umberto Eco',        'Roman policier',  3),
    (3, 'L''Etranger',                 'Albert Camus',       'Roman',           5),
    (4, 'Fondation',                   'Isaac Asimov',       'Science-fiction', 1),
    (5, 'Les Miserables',              'Victor Hugo',        'Roman',           2),
    (6, 'Clean Code',                  'Robert C. Martin',   'Informatique',    6);
