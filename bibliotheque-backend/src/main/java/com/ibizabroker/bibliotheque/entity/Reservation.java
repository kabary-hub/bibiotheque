package com.ibizabroker.bibliotheque.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Reservation d'un livre indisponible par un adherent.
 *
 * Cette entite ne sort jamais du service : les controleurs ne manipulent que
 * ReservationRequestDto et ReservationResponseDto.
 */
@Data
@Entity
@Table(name = "Reservation")
public class Reservation {

    /** RG-04 : duree de validite d'une reservation, en jours. */
    public static final int DUREE_VALIDITE_JOURS = 7;

    /**
     * IDENTITY et non AUTO : Books et Users utilisent GenerationType.AUTO, qui
     * sous Hibernate 5 partage une unique table hibernate_sequence. Y brancher
     * une troisieme entite ferait avancer le compteur commun a chaque
     * reservation. IDENTITY isole les identifiants, comme le font deja Borrow
     * et Role.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer reservationId;

    /**
     * optional = false pose la contrainte au niveau JPA, nullable = false au
     * niveau du schema SQL : la relation est obligatoire des deux cotes.
     * Le fetch reste EAGER (defaut de @ManyToOne) : le service a besoin du
     * titre du livre et du nom de l'adherent pour construire le DTO de sortie.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "livre_id", nullable = false)
    private Books livre;

    @ManyToOne(optional = false)
    @JoinColumn(name = "adherent_id", nullable = false)
    private Users adherent;

    /**
     * Horodatage serveur. updatable = false : la date de prise de reservation
     * est un fait, elle ne se corrige pas. Le client ne la fournit jamais,
     * elle n'apparait pas dans ReservationRequestDto.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateReservation;

    /** RG-04 : dateReservation + DUREE_VALIDITE_JOURS, calculee par le service. */
    @Column(nullable = false)
    private LocalDateTime dateExpiration;

    /**
     * EnumType.STRING et non ORDINAL : en ORDINAL, inserer une valeur au milieu
     * de l'enumeration reecrirait silencieusement le sens des lignes deja en
     * base. La colonne stocke « EN_ATTENTE », lisible en SQL.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutReservation statut;
}
