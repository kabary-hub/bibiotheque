import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { Books } from '../../_model/books';
import { Users } from '../../_model/users';
import { ErreurApi } from '../../_service/reservation.service';

/**
 * Formulaire de creation d'une reservation.
 *
 * Il ne connait ni l'API ni le service : il recoit les deux referentiels a
 * proposer, emet le couple choisi, et affiche l'erreur que le conteneur lui
 * repasse. C'est le conteneur qui appelle le serveur.
 */
@Component({
    selector: 'app-reservation-form',
    templateUrl: './reservation-form.component.html',
    styleUrls: ['./reservation-form.component.css']
})
export class ReservationFormComponent implements OnChanges {

    @Input() livres: Books[] = [];
    @Input() adherents: Users[] = [];
    @Input() envoiEnCours = false;
    @Input() erreur: ErreurApi | null = null;
    @Input() erreurReferentiels: ErreurApi | null = null;

    /**
     * Change de valeur apres chaque creation reussie. Le formulaire s'en sert
     * comme d'un signal pour se vider : le conteneur n'a pas besoin d'une
     * reference vers ce composant pour le reinitialiser.
     */
    @Input() jetonReinitialisation = 0;

    // RS-04 : seul livreId est envoye. L'identite de l'adherent vient du token JWT.
    @Output() soumettre = new EventEmitter<{ livreId: number }>();
    @Output() reessayerReferentiels = new EventEmitter<void>();

    livreId: number | null = null;

    ngOnChanges(changements: SimpleChanges): void {
        if (changements['jetonReinitialisation'] && !changements['jetonReinitialisation'].firstChange) {
            this.livreId = null;
        }
    }

    /** Le champ livreId est obligatoire : le bouton reste inactif sans lui. */
    get valide(): boolean {
        return this.livreId !== null;
    }

    /**
     * Libelle d'un livre dans la liste deroulante.
     *
     * Le nombre d'exemplaires y figure parce que c'est lui qui decide du sort
     * de la demande : un livre a 0 exemplaire est reservable, un livre en rayon
     * sera refuse par RG-01. Autant que l'utilisateur le sache avant de cliquer.
     */
    libelleLivre(livre: Books): string {
        const exemplaires = livre.noOfCopies === 0
            ? 'aucun exemplaire disponible'
            : `${livre.noOfCopies} exemplaire(s) en rayon`;
        return `${livre.bookName} — ${livre.bookAuthor} (${exemplaires})`;
    }

    envoyer(): void {
        if (!this.valide || this.envoiEnCours) {
            return;
        }
        this.soumettre.emit({
            livreId: this.livreId as number
        });
    }
}
