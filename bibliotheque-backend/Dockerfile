# =============================================================================
#  Backend — API Spring Boot
# =============================================================================
#  Construction en deux etapes :
#    - une image lourde (Maven + JDK complet) pour compiler ;
#    - une image legere (JRE seul) pour executer.
#  Seul le .jar produit passe d'une etape a l'autre : ni Maven, ni les sources,
#  ni le cache de dependances ne se retrouvent dans l'image finale.
# =============================================================================


# --- Etape 1 : construction -------------------------------------------------
#
#  JDK 11 et non 17 ou 21 : ce projet depend de Lombok 1.18.20, impose par
#  spring-boot-starter-parent 2.4.5. Cette version de Lombok manipule les API
#  internes du compilateur (com.sun.tools.javac.tree.JCTree) et echoue sur un
#  JDK recent avec :
#      NoSuchFieldError: Class JCTree$JCImport does not have member field
#      'com.sun.tools.javac.tree.JCTree qualid'
#  Construire avec un JDK 11 resout le probleme sans modifier le pom.xml.
#  Voir la section 1.3 du rapport pour l'analyse complete.
#
FROM maven:3.8-eclipse-temurin-11 AS build

WORKDIR /build

# Le pom seul d'abord : tant qu'il ne change pas, Docker reutilise le cache de
# cette couche et ne retelecharge pas les dependances a chaque build.
COPY pom.xml ./

# --mount=type=cache conserve le depot Maven local entre deux constructions,
# hors des couches de l'image. Si une construction est interrompue — coupure
# reseau, echec d'un autre service annulant celui-ci — la suivante reprend avec
# ce qui a deja ete telecharge au lieu de tout recommencer.
#
# Le cache reste portable : sur une machine qui n'a jamais vu le projet, il est
# simplement vide et tout est telecharge normalement. « --no-cache » invalide
# les couches, pas les caches de ce type.
#
# Les options wagon rendent chaque telechargement resistant aux coupures
# (Connection reset observe sur des connexions lentes).
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B dependency:go-offline \
      -Dmaven.wagon.http.retryHandler.count=5 \
      -Dmaven.wagon.http.pool=false

COPY src ./src

# Les tests sont ignores : BibliothequeApplicationTests charge le contexte
# Spring complet, donc exige une base MySQL joignable. Elle ne l'est pas
# pendant un build d'image, qui ne participe pas au reseau de compose.
#
# Le jar produit va dans /build/target/, hors du point de montage de cache : il
# est donc bien ecrit dans la couche et recuperable par l'etape suivante.
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B clean package -DskipTests \
      -Dmaven.wagon.http.retryHandler.count=5 \
      -Dmaven.wagon.http.pool=false


# --- Etape 2 : execution ----------------------------------------------------
FROM eclipse-temurin:11-jre

WORKDIR /app

# L'application ne tourne pas en root : si le processus est compromis, il n'a
# pas les droits d'ecriture sur le systeme de fichiers du conteneur.
RUN useradd --system --create-home --shell /usr/sbin/nologin spring
USER spring

COPY --from=build --chown=spring:spring /build/target/*.jar /app/app.jar

EXPOSE 8080

# Aucun parametre de connexion ici. L'URL, l'utilisateur et le mot de passe
# arrivent par variables d'environnement (SPRING_DATASOURCE_*), definies dans
# docker-compose.yml. Spring Boot les fait primer sur application.properties,
# qui n'est donc pas modifie.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
