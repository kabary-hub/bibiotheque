package com.ibizabroker.bibliotheque.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Set;

@Data
@Entity
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer userId;
    private String username;
    private String name;
    private String password;
    /**
     * Aucune cascade, volontairement.
     *
     * L'association portait CascadeType.ALL. Un role reconstruit par Jackson
     * depuis le JSON de la requete est une entite DETACHEE : la cascade PERSIST
     * tentait de l'inserer, et Hibernate refusait avec
     * « detached entity passed to persist ». POST /admin/users repondait 500 des
     * qu'un role etait fourni.
     *
     * Le fond du probleme depasse le symptome : « role » est un vocabulaire fixe,
     * pas une donnee possedee par l'utilisateur. Une cascade REMOVE aurait meme
     * supprime le role « Admin » de la base en supprimant un administrateur.
     * Les roles sont desormais toujours relus depuis la base avant d'etre
     * rattaches (voir UsersService).
     *
     * EAGER est conserve : JwtService a besoin des roles pour construire les
     * autorisations, et spring.jpa.open-in-view est desormais a false.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "USER_ROLE",
            joinColumns = {
                    @JoinColumn(name = "USER_ID")
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "ROLE_ID")
            }
    )
    private Set<Role> role;

}

