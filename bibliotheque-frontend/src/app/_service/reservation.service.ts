import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiError, Reservation, StatutReservation } from '../_model/reservation';

/**
 * Une erreur d'appel deja traduite en quelque chose d'affichable.
 *
 * Les composants ne manipulent jamais de HttpErrorResponse : ils recoivent ceci,
 * qui contient toujours un message en francais, et la reference de la regle de
 * gestion quand le serveur en a nomme une.
 */
export class ErreurApi {
    constructor(
        public statut: number,
        public message: string,
        public regle?: string
    ) { }
}

@Injectable({
    providedIn: 'root'
})
export class ReservationService {

    private baseURL = "http://localhost:8080/api/reservations";

    constructor(private httpClient: HttpClient) { }

    /**
     * Le filtre est applique par le serveur (GET /api/reservations?statut=),
     * pas par le navigateur : filtrer une liste deja chargee donnerait un
     * resultat different des le jour ou la liste sera paginee.
     */
    lister(statut?: StatutReservation): Observable<Reservation[]> {
        let params = new HttpParams();
        if (statut) {
            params = params.set('statut', statut);
        }
        return this.httpClient.get<Reservation[]>(this.baseURL, { params });
    }

    creer(livreId: number, adherentId: number): Observable<Reservation> {
        return this.httpClient.post<Reservation>(this.baseURL, { livreId, adherentId });
    }

    annuler(id: number): Observable<Reservation> {
        return this.httpClient.patch<Reservation>(`${this.baseURL}/${id}/annuler`, {});
    }

    supprimer(id: number): Observable<Object> {
        return this.httpClient.delete(`${this.baseURL}/${id}`);
    }

    /**
     * Traduit ce que rxjs a fait remonter en un message destine a l'utilisateur.
     *
     * Le cas normal est le premier : le serveur a repondu, son corps est un
     * ApiError, on affiche sa phrase telle quelle. Elle est deja redigee pour
     * un humain et nomme sa regle ("RG-03 : l'adherent ... a deja 3
     * reservation(s) active(s)"). La reecrire ici ferait diverger le message
     * affiche de celui que documente l'API.
     */
    static messageDe(erreur: unknown): ErreurApi {
        if (!(erreur instanceof HttpErrorResponse)) {
            return new ErreurApi(0, "Erreur inattendue du navigateur. Reessayez.");
        }

        // status 0 : la requete n'a jamais abouti — backend arrete, DNS, CORS.
        if (erreur.status === 0) {
            return new ErreurApi(0,
                "Le serveur est injoignable. Verifiez que le backend est demarre sur "
                + "http://localhost:8080, puis reessayez.");
        }

        const corps = erreur.error as ApiError;
        if (corps && corps.message) {
            return new ErreurApi(erreur.status, corps.message, corps.regle);
        }

        // Le serveur a repondu, mais pas avec un ApiError : 403 de Spring
        // Security, page d'erreur du proxy, HTML... On dit au moins quoi.
        return new ErreurApi(erreur.status,
            `Le serveur a repondu ${erreur.status} ${erreur.statusText} sans message exploitable.`);
    }
}
