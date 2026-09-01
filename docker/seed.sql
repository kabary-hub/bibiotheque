-- =============================================================================
--  Jeu de donnees initial — PostgreSQL
-- =============================================================================
--  Joue par le service « seed » de docker-compose.yml. Sans lui, l'application
--  demarre sur une base vide : aucun compte n'existe, et POST /admin/users
--  exige deja un jeton — on ne peut donc pas creer le premier administrateur
--  par l'API.
--
--  Ce script s'execute APRES le backend, et non avant : c'est Hibernate qui
--  cree les tables au demarrage (spring.jpa.hibernate.ddl-auto=update). Un
--  script place dans /docker-entrypoint-initdb.d s'executerait trop tot, sur
--  une base sans tables.
--
--  Noms de tables en minuscules : la strategie de nommage de Spring Boot
--  convertit @Table(name = "Books") en table « books ». PostgreSQL replie de
--  toute facon en minuscules tout identifiant non quote.
--
--  Rejouable : chaque insertion porte un ON CONFLICT DO NOTHING, l'equivalent
--  PostgreSQL de l'INSERT IGNORE de MySQL.
-- =============================================================================


-- --- Roles -------------------------------------------------------------------

INSERT INTO role (role_id, role_name) VALUES
    (1, 'Admin'),
    (2, 'User')
ON CONFLICT (role_id) DO NOTHING;


-- --- Comptes -----------------------------------------------------------------
-- Les mots de passe sont des hachages BCrypt : WebSecurityConfiguration declare
-- un BCryptPasswordEncoder, un mot de passe en clair serait rejete.
-- Le hachage ci-dessous correspond a « admin123 ».

INSERT INTO users (user_id, username, name, password) VALUES
    (1, 'admin',   'Administrateur',
        '$2b$10$RN5ij7XXjDpRBALhITW.2uzYGontX4U9c9ZRH5i3e.5l6RvkjZ696'),
    (2, 'lecteur', 'Lecteur de demonstration',
        '$2b$10$RN5ij7XXjDpRBALhITW.2uzYGontX4U9c9ZRH5i3e.5l6RvkjZ696')
ON CONFLICT (user_id) DO NOTHING;


-- --- Association compte / role ----------------------------------------------

INSERT INTO user_role (user_id, role_id) VALUES
    (1, 1),   -- admin   -> Admin
    (2, 2)    -- lecteur -> User
ON CONFLICT DO NOTHING;


-- =============================================================================
--  Le scenario de l'epreuve : 5 livres L1 a L5, 3 adherents A1 a A3
-- =============================================================================
--
--    L1              un livre DISPONIBLE, aucun emprunt en cours
--    L2, L3, L4, L5  quatre livres TOUS EMPRUNTES ET NON RENDUS
--    A1              le reservataire principal
--    A2              celui qui saturera son quota (RG-03)
--    A3              l'emprunteur : c'est lui qui detient L2 a L5
--
--  no_of_copies est la seule colonne que regarde RG-01 : > 0 vaut disponible
--  et donc non reservable, 0 vaut indisponible et donc reservable.
--
--  Les quatre livres sont a 0 exemplaire ET portent un emprunt non rendu par
--  A3 : la situation est coherente, les exemplaires sont chez un lecteur, pas
--  perdus.
-- =============================================================================

INSERT INTO books (book_id, book_name, book_author, book_genre, no_of_copies) VALUES
    (201, 'L1 - Le Comte de Monte-Cristo',  'Alexandre Dumas',        'Roman',  3),
    (202, 'L2 - Germinal',                  'Emile Zola',             'Roman',  0),
    (203, 'L3 - Madame Bovary',             'Gustave Flaubert',       'Roman',  0),
    (204, 'L4 - Les Fleurs du mal',         'Charles Baudelaire',     'Poesie', 0),
    (205, 'L5 - Voyage au bout de la nuit', 'Louis-Ferdinand Celine', 'Roman',  0)
ON CONFLICT (book_id) DO UPDATE SET
    book_name    = EXCLUDED.book_name,
    book_author  = EXCLUDED.book_author,
    book_genre   = EXCLUDED.book_genre,
    no_of_copies = EXCLUDED.no_of_copies;


INSERT INTO users (user_id, username, name, password) VALUES
    (301, 'a1_reservataire', 'A1 - Reservataire principal',
        '$2b$10$RN5ij7XXjDpRBALhITW.2uzYGontX4U9c9ZRH5i3e.5l6RvkjZ696'),
    (302, 'a2_quota',        'A2 - Saturera son quota',
        '$2b$10$RN5ij7XXjDpRBALhITW.2uzYGontX4U9c9ZRH5i3e.5l6RvkjZ696'),
    (303, 'a3_emprunteur',   'A3 - Emprunteur de L2 a L5',
        '$2b$10$RN5ij7XXjDpRBALhITW.2uzYGontX4U9c9ZRH5i3e.5l6RvkjZ696')
ON CONFLICT (user_id) DO UPDATE SET
    username = EXCLUDED.username,
    name     = EXCLUDED.name,
    password = EXCLUDED.password;

-- Role 2 = User. Sans ligne ici, l'adherent existe mais n'a aucun role : les
-- routes annotees @PreAuthorize("hasRole('Admin')") lui restent fermees, ce qui
-- est le comportement voulu pour un simple lecteur.
INSERT INTO user_role (user_id, role_id) VALUES
    (301, 2),
    (302, 2),
    (303, 2)
ON CONFLICT DO NOTHING;


-- --- Les emprunts en cours ---------------------------------------------------
--
-- « Emprunte et non rendu » se traduit par return_date IS NULL. C'est A3 qui
-- detient les quatre livres, emprunte il y a trois jours, echeance dans quatre.
--
-- La table borrow n'a pas de cle naturelle : on purge avant de reinserer,
-- sans quoi rejouer le script creerait des doublons.

DELETE FROM borrow WHERE book_id IN (202, 203, 204, 205);

INSERT INTO borrow (book_id, user_id, issue_date, due_date, return_date) VALUES
    (202, 303, NOW() - INTERVAL '3 days', NOW() + INTERVAL '4 days', NULL),
    (203, 303, NOW() - INTERVAL '3 days', NOW() + INTERVAL '4 days', NULL),
    (204, 303, NOW() - INTERVAL '3 days', NOW() + INTERVAL '4 days', NULL),
    (205, 303, NOW() - INTERVAL '3 days', NOW() + INTERVAL '4 days', NULL);


-- =============================================================================
--  Recalage des sequences
-- =============================================================================
--  Le piege propre a PostgreSQL, que MySQL n'avait pas.
--
--  Inserer un identifiant explicite n'avance pas la sequence qui l'alimente.
--  Apres ce script, la sequence des livres pointe toujours sur 1 alors que la
--  table contient un livre 205 : la premiere creation depuis l'application
--  echouerait sur une violation de cle primaire, et le message d'erreur ne
--  designerait pas la cause.
--
--  pg_get_serial_sequence retrouve la sequence associee a une colonne, quel que
--  soit le nom qu'Hibernate lui a donne. On l'avance au plus grand identifiant
--  reellement present.
-- =============================================================================

DO $$
DECLARE
    cible RECORD;
    seq   TEXT;
    maxi  BIGINT;
BEGIN
    FOR cible IN
        SELECT * FROM (VALUES
            ('books',       'book_id'),
            ('users',       'user_id'),
            ('role',        'role_id'),
            ('borrow',      'borrow_id'),
            ('reservation', 'reservation_id')
        ) AS t(table_name, id_column)
    LOOP
        seq := pg_get_serial_sequence(cible.table_name, cible.id_column);
        IF seq IS NOT NULL THEN
            EXECUTE format('SELECT COALESCE(MAX(%I), 0) FROM %I',
                           cible.id_column, cible.table_name) INTO maxi;
            -- Le troisieme argument dit si la valeur est « deja consommee ».
            -- Sur une table vide (maxi = 0) on pose 1 sans le consommer, pour
            -- que le premier identifiant soit 1 et non 2.
            PERFORM setval(seq, GREATEST(maxi, 1), maxi > 0);
            RAISE NOTICE '[seed] sequence % recalee sur % (consommee: %)',
                         seq, GREATEST(maxi, 1), maxi > 0;
        END IF;
    END LOOP;
END
$$;

-- Books et Users sont en GenerationType.AUTO : Hibernate ne leur cree pas de
-- sequence propre mais une sequence partagee, « hibernate_sequence », que
-- pg_get_serial_sequence ne voit pas. On l'avance au-dela des identifiants
-- poses a la main.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_class WHERE relname = 'hibernate_sequence'
                                        AND relkind = 'S') THEN
        PERFORM setval('hibernate_sequence', 1000);
        RAISE NOTICE '[seed] hibernate_sequence recalee sur 1000';
    END IF;
END
$$;
