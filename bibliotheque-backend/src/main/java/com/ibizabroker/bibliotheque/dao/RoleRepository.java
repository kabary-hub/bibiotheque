package com.ibizabroker.bibliotheque.dao;

import com.ibizabroker.bibliotheque.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Acces aux roles.
 *
 * Le projet n'en avait pas : les roles arrivaient dans le corps de la requete et
 * etaient enregistres tels quels, ce qui laissait un client inventer un role
 * inexistant, ou faire echouer l'enregistrement sur une entite detachee.
 * Ils sont desormais toujours relus depuis la base.
 */
public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByRoleName(String roleName);
}
