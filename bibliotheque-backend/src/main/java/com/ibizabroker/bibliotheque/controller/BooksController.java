package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dto.BookRequestDto;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.exceptions.ApiError;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * Catalogue des livres.
 *
 * La lecture est ouverte a tout compte authentifie, l'ecriture reservee au role
 * Admin. Ce n'est pas une negligence heritee : un lecteur a besoin de la liste
 * pour emprunter, et l'ecran de reservation a besoin du nombre d'exemplaires
 * pour expliquer un refus RG-01. La consultation d'un livre precis suivait
 * l'ecriture et exigeait Admin, ce qui interdisait a un lecteur d'ouvrir la
 * fiche d'un livre qu'il voyait pourtant dans la liste : l'incoherence est
 * levee.
 */
@CrossOrigin("http://localhost:4200")
@RestController
@RequestMapping("/admin/books")
@Tag(name = "Livres", description = "Catalogue des ouvrages")
public class BooksController {

    @Autowired
    private BooksRepository booksRepository;

    @Operation(summary = "Lister les livres")
    @ApiResponse(responseCode = "200", description = "Catalogue complet")
    @GetMapping
    public List<Books> lister() {
        return booksRepository.findAll();
    }

    @Operation(summary = "Consulter un livre")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Livre trouve"),
            @ApiResponse(responseCode = "404", description = "Livre inconnu",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public Books consulter(
            @Parameter(description = "Identifiant du livre", example = "201")
            @PathVariable Integer id) {
        return trouver(id);
    }

    @Operation(summary = "Ajouter un livre")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Livre cree"),
            @ApiResponse(responseCode = "400", description = "Champ obligatoire absent ou invalide",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Role Admin requis",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("hasRole('Admin')")
    @PostMapping
    public ResponseEntity<Books> creer(@Valid @RequestBody BookRequestDto demande) {
        Books livre = new Books();
        appliquer(demande, livre);
        // 201 et non 200 : la ressource n'existait pas avant l'appel.
        return ResponseEntity.status(HttpStatus.CREATED).body(booksRepository.save(livre));
    }

    @Operation(summary = "Modifier un livre")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Livre modifie"),
            @ApiResponse(responseCode = "400", description = "Champ obligatoire absent ou invalide",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Livre inconnu",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("hasRole('Admin')")
    @PutMapping("/{id}")
    public Books modifier(
            @Parameter(description = "Identifiant du livre", example = "201")
            @PathVariable Integer id,
            @Valid @RequestBody BookRequestDto demande) {
        Books livre = trouver(id);
        appliquer(demande, livre);
        return booksRepository.save(livre);
    }

    @Operation(summary = "Supprimer un livre")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Livre supprime"),
            @ApiResponse(responseCode = "404", description = "Livre inconnu",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("hasRole('Admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(
            @Parameter(description = "Identifiant du livre", example = "201")
            @PathVariable Integer id) {
        booksRepository.delete(trouver(id));
        // 204 et non un corps { "deleted": true } : le code HTTP porte deja
        // l'information, et un corps invente oblige le client a le lire.
        return ResponseEntity.noContent().build();
    }

    private Books trouver(Integer id) {
        return booksRepository.findById(id).orElseThrow(() -> new NotFoundException(
                String.format("Le livre d'identifiant %d n'existe pas.", id)));
    }

    private void appliquer(BookRequestDto demande, Books livre) {
        livre.setBookName(demande.getBookName());
        livre.setBookAuthor(demande.getBookAuthor());
        livre.setBookGenre(demande.getBookGenre());
        livre.setNoOfCopies(demande.getNoOfCopies());
    }
}
