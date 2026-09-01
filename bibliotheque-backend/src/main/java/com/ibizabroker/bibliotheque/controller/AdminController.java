package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dto.UserRequestDto;
import com.ibizabroker.bibliotheque.dto.UserResponseDto;
import com.ibizabroker.bibliotheque.exceptions.ApiError;
import com.ibizabroker.bibliotheque.service.UsersService;
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
 * Gestion des comptes.
 *
 * Toutes les operations exigent le role Admin. L'annotation manquait sur la
 * creation — elle etait commentee — ce qui laissait n'importe quel compte
 * authentifie, y compris un simple lecteur, creer un utilisateur et lui
 * accorder le role Admin. C'etait une elevation de privileges accessible en
 * une requete.
 */
@CrossOrigin("http://localhost:4200")
@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('Admin')")
@Tag(name = "Comptes", description = "Administration des comptes utilisateurs")
public class AdminController {

    @Autowired
    private UsersService usersService;

    @Operation(summary = "Lister les comptes",
            description = "Le mot de passe hache ne figure dans aucune reponse.")
    @ApiResponse(responseCode = "200", description = "Liste des comptes")
    @GetMapping
    public List<UserResponseDto> lister() {
        return usersService.lister();
    }

    @Operation(summary = "Consulter un compte")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte trouve"),
            @ApiResponse(responseCode = "404", description = "Compte inconnu",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public UserResponseDto consulter(
            @Parameter(description = "Identifiant du compte", example = "301")
            @PathVariable Integer id) {
        return usersService.consulter(id);
    }

    @Operation(summary = "Creer un compte",
            description = "Les roles sont designes par leur identifiant et relus depuis la base. "
                    + "Le mot de passe est hache par le serveur.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compte cree"),
            @ApiResponse(responseCode = "400", description = "Champ obligatoire absent ou invalide",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Role inconnu",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Nom d'utilisateur deja pris",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<UserResponseDto> creer(@Valid @RequestBody UserRequestDto demande) {
        // 201 et non 200 : la ressource n'existait pas avant l'appel.
        return ResponseEntity.status(HttpStatus.CREATED).body(usersService.creer(demande));
    }

    @Operation(summary = "Modifier un compte",
            description = "Un mot de passe absent du corps laisse l'ancien inchange.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte modifie"),
            @ApiResponse(responseCode = "400", description = "Champ obligatoire absent ou invalide",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Compte ou role inconnu",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Nom d'utilisateur deja pris",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    public UserResponseDto modifier(
            @Parameter(description = "Identifiant du compte", example = "301")
            @PathVariable Integer id,
            @Valid @RequestBody UserRequestDto demande) {
        return usersService.modifier(id, demande);
    }

    @Operation(summary = "Supprimer un compte",
            description = "Refuse si le compte est le dernier administrateur.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Compte supprime"),
            @ApiResponse(responseCode = "404", description = "Compte inconnu",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Dernier administrateur",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(
            @Parameter(description = "Identifiant du compte", example = "305")
            @PathVariable Integer id) {
        usersService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
