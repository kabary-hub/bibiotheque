package com.ibizabroker.bibliotheque.entity;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Cycle de vie d'une reservation.
 *
 * <pre>
 *   EN_ATTENTE ──► DISPONIBLE ──► HONOREE
 *        │              │
 *        ├──────────────┴──► ANNULEE   (RG-05)
 *        └──────────────┬──► EXPIREE   (RG-04, echeance depassee)
 * </pre>
 *
 * Les trois etats de droite sont terminaux : une fois atteints, plus aucune
 * transition n'est possible (RG-06).
 */
public enum StatutReservation {

    EN_ATTENTE,
    DISPONIBLE,
    ANNULEE,
    EXPIREE,
    HONOREE;

    /**
     * Statuts dits « actifs » au sens de l'enonce : EN_ATTENTE ou DISPONIBLE.
     * Sert de reference unique a RG-02 et RG-03, pour que la definition de
     * « reservation active » ne soit ecrite qu'a un seul endroit.
     */
    public static final Set<StatutReservation> ACTIFS =
            Collections.unmodifiableSet(EnumSet.of(EN_ATTENTE, DISPONIBLE));

    /** Statuts terminaux : ANNULEE, EXPIREE, HONOREE. Support de RG-06. */
    public static final Set<StatutReservation> TERMINAUX =
            Collections.unmodifiableSet(EnumSet.of(ANNULEE, EXPIREE, HONOREE));

    public boolean estActif() {
        return ACTIFS.contains(this);
    }

    /** RG-06 : un statut terminal interdit toute modification ulterieure. */
    public boolean estTerminal() {
        return TERMINAUX.contains(this);
    }

    public static Collection<StatutReservation> valeursAutorisees() {
        return Arrays.asList(values());
    }
}
