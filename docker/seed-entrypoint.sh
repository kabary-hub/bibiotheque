#!/bin/bash
# =============================================================================
#  Point d'entree du service « seed »
# =============================================================================
#  Attend que les tables existent, puis insere le jeu de donnees initial.
#
#  Pourquoi une attente active plutot qu'un depends_on ?
#  Le healthcheck de « db » garantit que MySQL repond, pas que les tables
#  existent : ce sont Hibernate et ddl-auto=update qui les creent, au demarrage
#  du backend. Compose ne sait pas attendre cet evenement-la. On interroge donc
#  la base jusqu'a ce que la table « books » soit interrogeable.
# =============================================================================

set -euo pipefail

DB_HOST="${DB_HOST:-db}"
DB_NAME="${MYSQL_DATABASE:-bibliotheque}"
DB_PASS="${MYSQL_ROOT_PASSWORD:-mysql}"

# Toutes les tables que seed.sql va manipuler, et non une seule.
#
# Hibernate les cree une par une : attendre « books » uniquement laissait
# passer le script alors que « users » n'existait pas encore, d'ou un
# « ERROR 1146: Table 'bibliotheque.users' doesn't exist » en pleine insertion.
# C'est une condition de course, invisible tant que l'ordre de creation est
# favorable.
REQUIRED_TABLES="books users role user_role"

MAX_ATTEMPTS=60
attempt=0

tables_ready() {
    for table in $REQUIRED_TABLES; do
        mysql -h "$DB_HOST" -u root -p"$DB_PASS" "$DB_NAME" \
              -e "SELECT 1 FROM \`$table\` LIMIT 1;" >/dev/null 2>&1 || return 1
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

mysql -h "$DB_HOST" -u root -p"$DB_PASS" "$DB_NAME" < /seed/seed.sql

# hibernate_sequence alimente les identifiants des entites en GenerationType.AUTO
# (Users et Books). Les lignes ci-dessus ayant ete inserees avec des identifiants
# explicites, il faut avancer le compteur au-dela, sinon la prochaine creation
# depuis l'application entrerait en collision avec une cle deja prise.
#
# L'echec est tolere : selon la version d'Hibernate, cette table peut ne pas
# exister, ce qui n'est pas une erreur.
mysql -h "$DB_HOST" -u root -p"$DB_PASS" "$DB_NAME" \
      -e "UPDATE hibernate_sequence SET next_val = 100 WHERE next_val < 100;" \
      >/dev/null 2>&1 || echo "[seed] Pas de table hibernate_sequence : ignore."

echo "[seed] Termine."
echo "[seed] Administrateur : admin / admin123"
echo "[seed] Lecteur        : lecteur / admin123"
