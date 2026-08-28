/**
 * Contrat du module Reservation, cote client.
 *
 * Les noms de champs reprennent exactement ceux de ReservationResponseDto :
 * toute divergence ici se traduirait par des cellules vides dans le tableau,
 * sans la moindre erreur a la compilation.
 */
export class Reservation {
    id: number;
    livreId: number;
    livreTitre: string;
    adherentId: number;
    adherentNom: string;
    /** Chaine ISO "yyyy-MM-ddTHH:mm:ss" imposee par @JsonFormat cote serveur. */
    dateReservation: string;
    dateExpiration: string;
    statut: StatutReservation;
}

/** Corps unique renvoye par ReservationExceptionHandler pour toute erreur. */
export class ApiError {
    timestamp: string;
    status: number;
    error: string;
    message: string;
    /** Renseigne uniquement pour un 409, absent du JSON sinon. */
    regle?: string;
}

export type StatutReservation =
    'EN_ATTENTE' | 'DISPONIBLE' | 'ANNULEE' | 'EXPIREE' | 'HONOREE';

/**
 * Le vocabulaire de l'enumeration serveur, avec son libelle francais et la
 * couleur de son badge.
 *
 * Ce n'est pas une donnee metier codee en dur : c'est le contrat de l'API, au
 * meme titre que le nom des champs ci-dessus. Les livres, les adherents et les
 * reservations, eux, viennent tous de l'API sans exception.
 */
export const STATUTS: { code: StatutReservation, libelle: string, classe: string }[] = [
    { code: 'EN_ATTENTE', libelle: 'En attente', classe: 'bg-secondary' },
    { code: 'DISPONIBLE', libelle: 'Disponible', classe: 'bg-success' },
    { code: 'ANNULEE', libelle: 'Annulee', classe: 'bg-dark' },
    { code: 'EXPIREE', libelle: 'Expiree', classe: 'bg-warning text-dark' },
    { code: 'HONOREE', libelle: 'Honoree', classe: 'bg-primary' }
];

/** RG-05 : seuls ces deux statuts autorisent une annulation. */
export const STATUTS_ANNULABLES: StatutReservation[] = ['EN_ATTENTE', 'DISPONIBLE'];
