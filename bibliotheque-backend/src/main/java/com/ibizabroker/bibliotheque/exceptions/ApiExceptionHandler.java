package com.ibizabroker.bibliotheque.exceptions;

import com.ibizabroker.bibliotheque.entity.StatutReservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Traduit toute exception de l'application en une reponse HTTP au format ApiError.
 *
 * L'advice est GLOBAL. Il l'etait auparavant limite au seul ReservationController,
 * pour ne pas changer le corps des erreurs des modules plus anciens. Cette reserve
 * n'a plus lieu d'etre : le projet doit repondre d'une seule voix, et les autres
 * modules renvoyaient jusqu'ici la page d'erreur par defaut de Spring — un corps
 * different a chaque cas, impossible a lire pour un client.
 *
 * Un principe gouverne l'ensemble : le client ne doit jamais recevoir 500 pour une
 * situation que le serveur sait nommer. Un identifiant inconnu vaut 404, une regle
 * enfreinte vaut 409, une saisie invalide vaut 400. Le 500 est reserve a ce que
 * personne n'a prevu, et lui seul est journalise avec sa pile.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    // =========================================================================
    //  400 — la requete est mal formee
    // =========================================================================

    /**
     * Champ obligatoire absent ou invalide.
     * Le message nomme chaque champ fautif : plusieurs champs absents produisent
     * une liste, pas un message tronque au premier.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> champInvalide(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .distinct()
                .collect(Collectors.joining(" "));
        return reponse(HttpStatus.BAD_REQUEST, message, null);
    }

    /**
     * Valeur de parametre inexploitable, typiquement ?statut=FOO.
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

    /** Parametre de requete obligatoire absent. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> parametreManquant(MissingServletRequestParameterException exception) {
        return reponse(HttpStatus.BAD_REQUEST,
                String.format("Le parametre '%s' est obligatoire.", exception.getParameterName()), null);
    }

    /** Corps de requete absent ou JSON mal forme. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> corpsIllisible(HttpMessageNotReadableException exception) {
        return reponse(HttpStatus.BAD_REQUEST,
                "Le corps de la requete est absent ou mal forme.", null);
    }

    // =========================================================================
    //  403 / 404
    // =========================================================================

    /**
     * Authentifie, mais sans le role exige.
     *
     * Sans ce cas, @PreAuthorize laisse remonter l'exception jusqu'a Spring
     * Security, qui repond une page d'erreur sans corps exploitable. Le client
     * ne sait alors pas distinguer « connecte-toi » de « tu n'as pas le droit ».
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accesRefuse(AccessDeniedException exception) {
        return reponse(HttpStatus.FORBIDDEN,
                "Vous etes authentifie mais votre role ne permet pas cette operation.", null);
    }

    /** La ressource visee n'existe pas. */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> introuvable(NotFoundException exception) {
        return reponse(HttpStatus.NOT_FOUND, exception.getMessage(), null);
    }

    /**
     * Optional.get() sur un Optional vide.
     *
     * Le code existant appelle .get() sans verifier (BorrowController), ce qui
     * produisait une 500 pour un identifiant simplement inconnu. Les appels sont
     * corriges, mais ce filet reste : il vaut mieux un 404 exact qu'une 500 pour
     * un cas oublie.
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> elementAbsent(NoSuchElementException exception) {
        return reponse(HttpStatus.NOT_FOUND,
                "La ressource demandee n'existe pas.", null);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiError> cheminInconnu(NoHandlerFoundException exception) {
        return reponse(HttpStatus.NOT_FOUND,
                String.format("Aucune ressource a l'adresse %s.", exception.getRequestURL()), null);
    }

    // =========================================================================
    //  409 — l'etat du systeme s'oppose a la demande
    // =========================================================================

    /** Regle de gestion enfreinte. Le corps nomme la regle dans un champ a part. */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> regleEnfreinte(BusinessRuleException exception) {
        return reponse(HttpStatus.CONFLICT, exception.getMessage(), exception.getRegle());
    }

    /**
     * Contrainte de la base violee : unicite, cle etrangere, colonne non nulle.
     *
     * C'est typiquement un nom d'utilisateur deja pris. Renvoyer 500 laisserait
     * croire a une panne alors que la demande est simplement refusee. Le detail
     * technique de la contrainte n'est pas expose : il nomme des colonnes et des
     * index qui ne regardent pas le client.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> contrainteViolee(DataIntegrityViolationException exception) {
        log.warn("Contrainte de base violee : {}", exception.getMostSpecificCause().getMessage());
        return reponse(HttpStatus.CONFLICT,
                "L'operation entre en conflit avec une donnee existante. "
                        + "Verifiez notamment l'unicite du nom d'utilisateur.", null);
    }

    // =========================================================================
    //  500 — dernier recours
    // =========================================================================

    /**
     * Tout le reste.
     *
     * La pile est journalisee cote serveur, jamais renvoyee au client : elle
     * revele les versions des bibliotheques et la structure interne. Le client
     * recoit une phrase neutre.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> imprevu(Exception exception) {
        log.error("Erreur non geree", exception);
        return reponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Une erreur interne est survenue. Elle a ete journalisee cote serveur.", null);
    }

    private ResponseEntity<ApiError> reponse(HttpStatus statut, String message, String regle) {
        return ResponseEntity.status(statut)
                .body(new ApiError(statut.value(), statut.getReasonPhrase(), message, regle));
    }
}
