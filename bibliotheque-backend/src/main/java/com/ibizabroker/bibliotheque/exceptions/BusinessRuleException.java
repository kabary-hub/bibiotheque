package com.ibizabroker.bibliotheque.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Violation d'une regle de gestion : la requete est bien formee et vise des
 * ressources qui existent, mais l'etat du systeme interdit l'operation.
 *
 * D'ou 409 CONFLICT et non 400 : ce n'est pas la requete qui est fautive, c'est
 * le moment. La meme requete rejouee plus tard peut reussir.
 *
 * La reference de la regle est portee comme un champ a part, et pas seulement
 * noyee dans le message : le client peut ainsi reagir a « RG-03 » sans analyser
 * du texte libre.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class BusinessRuleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String regle;

    public BusinessRuleException(String regle, String message) {
        super(regle + " : " + message);
        this.regle = regle;
    }

    public String getRegle() {
        return regle;
    }
}
