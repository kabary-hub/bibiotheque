import { Component, EventEmitter, Input, Output } from '@angular/core';
import {
    Reservation, StatutReservation, STATUTS, STATUTS_ANNULABLES
} from '../../_model/reservation';

/**
 * Presentation du tableau des reservations.
 *
 * Ce composant n'appelle rien et ne detient aucun etat : il recoit les lignes
 * a afficher et signale au conteneur qu'une annulation est demandee. C'est ce
 * qui permet de le rendre avec n'importe quel jeu de donnees, y compris dans
 * un test, sans backend.
 */
@Component({
    selector: 'app-reservation-list',
    templateUrl: './reservation-list.component.html',
    styleUrls: ['./reservation-list.component.css']
})
export class ReservationListComponent {

    @Input() reservations: Reservation[] = [];
    @Input() isAdmin = false;

    @Output() annulationDemandee = new EventEmitter<Reservation>();
    @Output() suppressionDemandee = new EventEmitter<Reservation>();

    libelle(statut: StatutReservation): string {
        const trouve = STATUTS.find((s) => s.code === statut);
        // Un statut ajoute cote serveur et pas encore connu ici s'affiche
        // tel quel plutot que de laisser une cellule vide.
        return trouve ? trouve.libelle : statut;
    }

    classeBadge(statut: StatutReservation): string {
        const trouve = STATUTS.find((s) => s.code === statut);
        return trouve ? trouve.classe : 'badge--neutral';
    }

    /**
     * RG-05, cote affichage : le bouton n'existe que pour les statuts que le
     * serveur accepte d'annuler. Ce n'est pas la regle — le serveur reste seul
     * juge et repond 409 si l'etat a change entre-temps — c'est seulement le
     * moyen de ne pas proposer une action vouee a echouer.
     */
    estAnnulable(statut: StatutReservation): boolean {
        return STATUTS_ANNULABLES.indexOf(statut) !== -1;
    }

    demanderAnnulation(reservation: Reservation): void {
        this.annulationDemandee.emit(reservation);
    }

    demanderSuppression(reservation: Reservation): void {
        this.suppressionDemandee.emit(reservation);
    }
}
