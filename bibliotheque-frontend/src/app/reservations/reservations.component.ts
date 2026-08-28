import { Component, OnInit } from '@angular/core';
import { Books } from '../_model/books';
import { Users } from '../_model/users';
import { Reservation, StatutReservation, STATUTS } from '../_model/reservation';
import { BooksService } from '../_service/books.service';
import { UsersService } from '../_service/users.service';
import { ErreurApi, ReservationService } from '../_service/reservation.service';

/**
 * Les quatre etats de l'ecran. Ils sont exclusifs : a tout instant le
 * gabarit en affiche un et un seul, ce qui rend impossible le cas ou un
 * tableau vide cohabite avec un indicateur de chargement.
 */
export type EtatListe = 'chargement' | 'donnees' | 'vide' | 'erreur';

/**
 * Conteneur de l'ecran de gestion des reservations.
 *
 * Il detient l'etat et declenche tous les appels ; la liste et le formulaire
 * sont des composants de presentation qui recoivent des donnees et emettent
 * des intentions. Aucun HttpClient n'est injecte ici : tout passe par les
 * services de src/app/_service.
 */
@Component({
    selector: 'app-reservations',
    templateUrl: './reservations.component.html',
    styleUrls: ['./reservations.component.css']
})
export class ReservationsComponent implements OnInit {

    etat: EtatListe = 'chargement';
    reservations: Reservation[] = [];
    erreurListe: ErreurApi | null = null;

    /** Chaine vide = "tous". Le filtre est envoye au serveur, pas applique ici. */
    filtreStatut: StatutReservation | '' = '';
    statuts = STATUTS;

    // --- Referentiels du formulaire -------------------------------------
    livres: Books[] = [];
    adherents: Users[] = [];
    referentielsCharges = false;
    erreurReferentiels: ErreurApi | null = null;

    // --- Creation --------------------------------------------------------
    creationEnCours = false;
    erreurCreation: ErreurApi | null = null;
    messageSucces: string | null = null;
    /** Incremente apres chaque creation reussie pour vider le formulaire. */
    jetonReinitialisation = 0;

    // --- Annulation ------------------------------------------------------
    aAnnuler: Reservation | null = null;
    annulationEnCours = false;
    erreurAnnulation: ErreurApi | null = null;

    constructor(
        private reservationService: ReservationService,
        private booksService: BooksService,
        private usersService: UsersService
    ) { }

    ngOnInit(): void {
        this.chargerReferentiels();
        this.charger();
    }

    // =====================================================================
    // Liste
    // =====================================================================

    charger(): void {
        this.etat = 'chargement';
        this.erreurListe = null;

        this.reservationService.lister(this.filtreStatut || undefined).subscribe({
            next: (donnees) => {
                this.reservations = donnees;
                this.etat = donnees.length === 0 ? 'vide' : 'donnees';
            },
            error: (err) => {
                this.erreurListe = ReservationService.messageDe(err);
                this.etat = 'erreur';
            }
        });
    }

    surChangementFiltre(): void {
        this.charger();
    }

    // =====================================================================
    // Referentiels : livres et adherents des listes deroulantes
    // =====================================================================

    /**
     * La liste des livres n'est volontairement pas restreinte aux livres
     * indisponibles. Masquer les livres en rayon rendrait le refus RG-01
     * inatteignable depuis l'ecran : la regle serait appliquee par le
     * navigateur au lieu d'etre appliquee par le serveur, et le message que
     * l'API prend soin de rediger ne s'afficherait jamais. Le nombre
     * d'exemplaires est affiche dans chaque option pour que le choix soit
     * eclaire.
     */
    chargerReferentiels(): void {
        this.erreurReferentiels = null;
        this.referentielsCharges = false;

        this.booksService.getBooksList().subscribe({
            next: (livres) => {
                this.livres = livres;
                this.marquerReferentielsCharges();
            },
            error: (err) => this.erreurReferentiels = ReservationService.messageDe(err)
        });

        this.usersService.getUsersList().subscribe({
            next: (adherents) => {
                this.adherents = adherents;
                this.marquerReferentielsCharges();
            },
            error: (err) => this.erreurReferentiels = ReservationService.messageDe(err)
        });
    }

    private marquerReferentielsCharges(): void {
        this.referentielsCharges = this.livres.length > 0 && this.adherents.length > 0;
    }

    // =====================================================================
    // Creation
    // =====================================================================

    creer(demande: { livreId: number, adherentId: number }): void {
        this.creationEnCours = true;
        this.erreurCreation = null;
        this.messageSucces = null;

        this.reservationService.creer(demande.livreId, demande.adherentId).subscribe({
            next: (creee) => {
                this.creationEnCours = false;
                this.jetonReinitialisation++;
                this.messageSucces =
                    `Reservation n°${creee.id} creee pour ${creee.adherentNom} `
                    + `sur « ${creee.livreTitre} ».`;

                // Une reservation nait EN_ATTENTE. Si un filtre sur un autre
                // statut est actif, elle n'apparaitrait pas dans la liste
                // rechargee et l'ecran semblerait n'avoir rien fait. On revient
                // donc a "tous" avant de recharger.
                if (this.filtreStatut !== '' && this.filtreStatut !== 'EN_ATTENTE') {
                    this.filtreStatut = '';
                }
                this.charger();
            },
            error: (err) => {
                this.creationEnCours = false;
                this.erreurCreation = ReservationService.messageDe(err);
            }
        });
    }

    // =====================================================================
    // Annulation
    // =====================================================================

    demanderAnnulation(reservation: Reservation): void {
        this.aAnnuler = reservation;
        this.erreurAnnulation = null;
    }

    fermerConfirmation(): void {
        this.aAnnuler = null;
        this.annulationEnCours = false;
    }

    confirmerAnnulation(): void {
        if (!this.aAnnuler) {
            return;
        }
        const cible = this.aAnnuler;
        this.annulationEnCours = true;
        this.erreurAnnulation = null;
        this.messageSucces = null;

        this.reservationService.annuler(cible.id).subscribe({
            next: (misAJour) => {
                this.annulationEnCours = false;
                this.aAnnuler = null;
                this.messageSucces = `Reservation n°${misAJour.id} annulee.`;

                // Le serveur renvoie la reservation telle qu'elle est desormais :
                // on remplace la ligne concernee au lieu de recharger toute la
                // liste. Le statut change sous les yeux de l'utilisateur.
                this.reservations = this.reservations.map(
                    (r) => r.id === misAJour.id ? misAJour : r);
            },
            error: (err) => {
                this.annulationEnCours = false;
                this.erreurAnnulation = ReservationService.messageDe(err);
            }
        });
    }

    effacerMessages(): void {
        this.messageSucces = null;
        this.erreurCreation = null;
    }
}
