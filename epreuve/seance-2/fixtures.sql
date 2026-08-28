-- =============================================================================
--  Jeu de donnees de test du module Reservation
-- =============================================================================
--  Reproduit le tableau de l'enonce :
--
--    L1              un livre DISPONIBLE, aucun emprunt en cours
--    L2, L3, L4, L5  quatre livres TOUS EMPRUNTES ET NON RENDUS
--    A1              l'adherent reservataire principal
--    A2              l'adherent qui saturera son quota (RG-03)
--    A3              l'emprunteur : c'est lui qui detient L2 a L5
--
--  Rejouable : les livres et adherents passent par ON DUPLICATE KEY UPDATE, et
--  les emprunts sont purges avant reinsertion (la table borrow n'a pas de cle
--  naturelle, un simple INSERT creerait des doublons a chaque execution).
--
--  Mot de passe des trois adherents : admin123
-- =============================================================================


-- --- Les livres --------------------------------------------------------------
--
-- no_of_copies est la seule chose que regarde RG-01 : > 0 vaut disponible,
-- 0 vaut indisponible et donc reservable.

INSERT INTO books (book_id, book_name, book_author, book_genre, no_of_copies) VALUES
    (201, 'L1 - Le Comte de Monte-Cristo',   'Alexandre Dumas',    'Roman',  3),
    (202, 'L2 - Germinal',                   'Emile Zola',         'Roman',  0),
    (203, 'L3 - Madame Bovary',              'Gustave Flaubert',   'Roman',  0),
    (204, 'L4 - Les Fleurs du mal',          'Charles Baudelaire', 'Poesie', 0),
    (205, 'L5 - Voyage au bout de la nuit',  'Louis-Ferdinand Celine', 'Roman', 0)
ON DUPLICATE KEY UPDATE
    book_name    = VALUES(book_name),
    book_author  = VALUES(book_author),
    book_genre   = VALUES(book_genre),
    no_of_copies = VALUES(no_of_copies);


-- --- Les adherents -----------------------------------------------------------
--
-- Le hachage est celui de « admin123 ». WebSecurityConfiguration declare un
-- BCryptPasswordEncoder : un mot de passe en clair serait rejete a la connexion.

INSERT INTO users (user_id, username, name, password) VALUES
    (301, 'a1_reservataire', 'A1 - Reservataire principal',
         '$2b$10$RN5ij7XXjDpRBALhITW.2uzYGontX4U9c9ZRH5i3e.5l6RvkjZ696'),
    (302, 'a2_quota',        'A2 - Saturera son quota',
         '$2b$10$RN5ij7XXjDpRBALhITW.2uzYGontX4U9c9ZRH5i3e.5l6RvkjZ696'),
    (303, 'a3_emprunteur',   'A3 - Emprunteur de L2 a L5',
         '$2b$10$RN5ij7XXjDpRBALhITW.2uzYGontX4U9c9ZRH5i3e.5l6RvkjZ696')
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    name     = VALUES(name),
    password = VALUES(password);

-- Role 2 = User (role 1 = Admin). Sans ligne ici, l'adherent existe mais
-- n'a aucun role : les routes annotees @PreAuthorize("hasRole('Admin')") lui
-- restent fermees, ce qui est le comportement voulu pour un simple lecteur.
INSERT IGNORE INTO user_role (user_id, role_id) VALUES
    (301, 2),
    (302, 2),
    (303, 2);


-- --- Les emprunts en cours ---------------------------------------------------
--
-- « Emprunte et non rendu » se traduit par return_date IS NULL. C'est A3 qui
-- detient les quatre livres, emprunte il y a trois jours, echeance dans quatre.

DELETE FROM borrow WHERE book_id IN (202, 203, 204, 205);

INSERT INTO borrow (book_id, user_id, issue_date, due_date, return_date) VALUES
    (202, 303, NOW() - INTERVAL 3 DAY, NOW() + INTERVAL 4 DAY, NULL),
    (203, 303, NOW() - INTERVAL 3 DAY, NOW() + INTERVAL 4 DAY, NULL),
    (204, 303, NOW() - INTERVAL 3 DAY, NOW() + INTERVAL 4 DAY, NULL),
    (205, 303, NOW() - INTERVAL 3 DAY, NOW() + INTERVAL 4 DAY, NULL);


-- --- Verification ------------------------------------------------------------

SELECT book_id, book_name, no_of_copies,
       CASE WHEN no_of_copies > 0 THEN 'DISPONIBLE' ELSE 'indisponible' END AS etat
FROM books WHERE book_id BETWEEN 201 AND 205 ORDER BY book_id;

SELECT u.user_id, u.username, u.name, r.role_name
FROM users u LEFT JOIN user_role ur ON ur.user_id = u.user_id
             LEFT JOIN role r ON r.role_id = ur.role_id
WHERE u.user_id BETWEEN 301 AND 303 ORDER BY u.user_id;

SELECT b.borrow_id, b.book_id, bk.book_name, b.user_id, u.username,
       b.issue_date, b.due_date, b.return_date
FROM borrow b JOIN books bk ON bk.book_id = b.book_id
              JOIN users u ON u.user_id = b.user_id
WHERE b.book_id BETWEEN 202 AND 205 ORDER BY b.book_id;
