# =============================================================================
#  Frontend — interface Angular
# =============================================================================
#  Angular ne s'execute pas cote serveur : `ng build` produit des fichiers
#  statiques (HTML, CSS, JS). Il n'y a donc rien a "faire tourner" ensuite,
#  seulement des fichiers a servir. D'ou les deux etapes : Node pour compiler,
#  nginx pour servir.
# =============================================================================


# --- Etape 1 : construction -------------------------------------------------
#
#  Node 16 et non 20 ou 24 : package.json declare Angular 14, dont les versions
#  de Node supportees sont 14.15+ et 16.10+. Node 24 (present sur le poste de
#  developpement) est six versions majeures trop recent. Fixer la version ici
#  garantit un build reproductible, quelle que soit la machine.
#
FROM node:16-alpine AS build

WORKDIR /build

# Les manifestes seuls d'abord : la couche npm est ainsi mise en cache et n'est
# rejouee que si les dependances changent, pas a chaque modification de source.
COPY package.json package-lock.json ./

# Reglages reseau avant l'installation.
#
# Les valeurs par defaut de npm (60s de delai, 2 tentatives) sont trop courtes
# sur une connexion lente : l'installation echoue avec ERR_SOCKET_TIMEOUT au
# bout de plusieurs minutes, alors que le telechargement progressait. On allonge
# le delai et on augmente le nombre de reprises.
RUN npm config set fetch-timeout 900000 \
 && npm config set fetch-retries 8 \
 && npm config set fetch-retry-mintimeout 20000 \
 && npm config set fetch-retry-maxtimeout 180000

# npm ci installe exactement ce que decrit package-lock.json. On retombe sur
# npm install si le lock est desynchronise du package.json.
RUN npm ci --no-audit --no-fund || npm install --no-audit --no-fund

COPY . .

# La configuration `production` est celle par defaut (angular.json, ligne 63).
RUN npm run build


# --- Etape 2 : service des fichiers -----------------------------------------
FROM nginx:alpine

# Sans cette configuration, recharger la page sur /books renverrait un 404 :
# nginx chercherait un fichier de ce nom, qui n'existe pas. Le routage est
# gere par Angular dans le navigateur, pas par le serveur.
COPY nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=build /build/dist/bibliotheque-frontend /usr/share/nginx/html

EXPOSE 80

# nginx:alpine demarre deja nginx au premier plan : pas de CMD a redefinir.
