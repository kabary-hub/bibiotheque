#!/bin/sh
# =============================================================================
#  Point d'entree du service « seed »
# =============================================================================
#  Attend que les tables existent, puis insere le jeu de donnees initial.
#
#  Pourquoi une attente active plutot qu'un depends_on ?
#  Le healthcheck de « db » garantit que PostgreSQL repond, pas que les tables
#  existent : ce sont Hibernate et ddl-auto=update qui les creent, au demarrage
#  du backend. Compose ne sait pas attendre cet evenement-la. On interroge donc
#  la base jusqu'a ce que chaque table soit interrogeable.
#
#  /bin/sh et non /bin/bash : postgres:16-alpine n'embarque pas bash.
# =============================================================================

set -eu

DB_HOST="${DB_HOST:-db}"
DB_NAME="${POSTGRES_DB:-bibliotheque}"
DB_USER="${POSTGRES_USER:-bibliotheque}"
# PGPASSWORD est fournie par docker-compose.yml et lue directement par psql.

PSQL="psql -h $DB_HOST -U $DB_USER -d $DB_NAME -v ON_ERROR_STOP=1"

# Toutes les tables que seed.sql va manipuler, et non une seule.
#
# Hibernate les cree une par une : attendre « books » uniquement laissait
# passer le script alors que « users » n'existait pas encore, d'ou un echec en
# pleine insertion. C'est une condition de course, invisible tant que l'ordre
# de creation reste favorable.
REQUIRED_TABLES="books users role user_role borrow reservation"

MAX_ATTEMPTS=60
attempt=0

tables_ready() {
    for table in $REQUIRED_TABLES; do
        $PSQL -tAc "SELECT to_regclass('public.$table');" 2>/dev/null \
            | grep -q "^$table$" || return 1
    done
    return 0
}

echo "[seed] Attente de la creation des tables par Hibernate..."
echo "[seed] Tables requises : $REQUIRED_TABLES"

until tables_ready
do
    attempt=$((attempt + 1))
    if [ "$attempt" -ge "$MAX_ATTEMPTS" ]; then
        echo "[seed] ECHEC : les tables ne sont toujours pas toutes presentes apres $((MAX_ATTEMPTS * 3))s." >&2
        echo "[seed] Verifiez que le backend a bien demarre : docker compose logs backend" >&2
        exit 1
    fi
    sleep 3
done

echo "[seed] Tables detectees. Insertion du jeu de donnees initial."

# ON_ERROR_STOP=1 : sans lui, psql poursuit apres une instruction en echec et
# sort avec le code 0. Le service paraitrait avoir reussi sur une base a moitie
# remplie.
$PSQL -f /seed/seed.sql

echo "[seed] Termine."
echo "[seed] Administrateur : admin / admin123"
echo "[seed] Lecteur        : lecteur / admin123"
echo "[seed] Adherents de l'epreuve : a1_reservataire, a2_quota, a3_emprunteur / admin123"
