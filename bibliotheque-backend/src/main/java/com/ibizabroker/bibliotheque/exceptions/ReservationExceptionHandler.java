package com.ibizabroker.bibliotheque.exceptions;

import com.ibizabroker.bibliotheque.controller.ReservationController;
import com.ibizabroker.bibliotheque.entity.StatutReservation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * Traduit les exceptions du module Reservation en reponses HTTP explicites.
 *
 * L'advice est volontairement limite a ReservationController (assignableTypes)
 * et non applique a tout le projet : un advice global changerait le corps des
 * erreurs deja renvoyees par AdminController, BooksController et
 * BorrowController, sur lesquelles le frontend Angular s'appuie deja.
 * Le module nouveau gagne une gestion d'erreurs propre sans rien casser autour.
 */
@RestControllerAdvice(assignableTypes = ReservationController.class)
public class ReservationExceptionHandler {

    /**
     * 400 — champ obligatoire absent.
     * Le message nomme le champ manquant, comme l'exige l'enonce : plusieurs
     * champs absents produisent une liste, pas un message tronque au premier.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> champManquant(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .distinct()
                .collect(Collectors.joining(" "));
        return reponse(HttpStatus.BAD_REQUEST, message, null);
    }

    /**
     * 400 — valeur de parametre inexploitable, typiquement ?statut=FOO.
     * Sans ce cas, Spring renverrait une 500 pour une simple faute de frappe.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> parametreInvalide(MethodArgumentTypeMismatchException exception) {
        String message;
        if (exception.getRequiredType() != null && exception.getRequiredType().isEnum()) {
            message = String.format(
                    "La valeur '%s' n'est pas un statut valide. Valeurs acceptees : %s.",
                    exception.getValue(), StatutReservation.valeursAutorisees());
        } else {
            message = String.format(
                    "Le parametre '%s' est invalide : '%s'.",
                    exception.getName(), exception.getValue());
        }
        return reponse(HttpStatus.BAD_REQUEST, message, null);
    }

    /** 400 — corps de requete absent ou JSON mal forme. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> corpsIllisible(HttpMessageNotReadableException exception) {
        return reponse(HttpStatus.BAD_REQUEST,
                "Le corps de la requete est absent ou mal forme. "
                        + "Attendu : {\"livreId\": 1, \"adherentId\": 2}.", null);
    }

    /** 404 — le livre, l'adherent ou la reservation vise n'existe pas. */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> introuvable(NotFoundException exception) {
        return reponse(HttpStatus.NOT_FOUND, exception.getMessage(), null);
    }

    /** 409 — regle de gestion enfreinte. Le corps nomme la regle. */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> regleEnfreinte(BusinessRuleException exception) {
        return reponse(HttpStatus.CONFLICT, exception.getMessage(), exception.getRegle());
    }

    private ResponseEntity<ApiError> reponse(HttpStatus statut, String message, String regle) {
        return ResponseEntity.status(statut)
                .body(new ApiError(statut.value(), statut.getReasonPhrase(), message, regle));
    }
}
