import { Component, OnInit } from '@angular/core';
import { Books } from '../_model/books';
import { Borrow } from '../_model/borrow';
import { Reservation, STATUTS } from '../_model/reservation';
import { BooksService } from '../_service/books.service';
import { BorrowService } from '../_service/borrow.service';
import { ErreurApi, ReservationService } from '../_service/reservation.service';
import { UserAuthService } from '../_service/user-auth.service';

@Component({
  selector: 'app-my-books',
  templateUrl: './my-books.component.html',
  styleUrls: ['./my-books.component.css']
})
export class MyBooksComponent implements OnInit {

  // --- Emprunts ---
  borrows: Borrow[] | null = null;
  returningId: number | null = null;

  // --- Réservations ---
  reservations: Reservation[] | null = null;
  livres: Books[] = [];
  livreIdChoisi: number | null = null;
  reservationEnCours = false;
  erreurReservation: ErreurApi | null = null;
  jetonReinit = 0;
  statuts = STATUTS;

  // --- Messages globaux ---
  succes: string | null = null;
  erreur: string | null = null;

  private userId: number = this.userAuthService.getUserId();

  constructor(
    private borrowService: BorrowService,
    private booksService: BooksService,
    private reservationService: ReservationService,
    private userAuthService: UserAuthService
  ) {}

  ngOnInit(): void {
    this.chargerEmprunts();
    this.chargerReservations();
    this.chargerLivres();
  }

  // =========================================================================
  // Emprunts
  // =========================================================================

  chargerEmprunts(): void {
    this.borrowService.getBooksBorrowedByUser(this.userId).subscribe({
      next: (data) => { this.borrows = data; },
      error: () => { this.borrows = []; }
    });
  }

  get empruntsActifs(): Borrow[] {
    return (this.borrows ?? []).filter(b => !b.returnDate);
  }

  get historiqueRendu(): Borrow[] {
    return (this.borrows ?? []).filter(b => !!b.returnDate);
  }

  isOverdue(b: Borrow): boolean {
    if (b.returnDate) { return false; }
    return b.dueDate ? new Date(b.dueDate) < new Date() : false;
  }

  returnBook(borrowId: number): void {
    this.succes = null;
    this.erreur = null;
    this.returningId = borrowId;
    this.borrowService.returnBook(borrowId).subscribe({
      next: () => {
        this.returningId = null;
        this.succes = 'Book returned successfully!';
        this.chargerEmprunts();
      },
      error: (err) => {
        this.returningId = null;
        this.erreur = err?.error?.message || 'Failed to return book.';
      }
    });
  }

  // =========================================================================
  // Réservations
  // =========================================================================

  chargerLivres(): void {
    this.booksService.getBooksList().subscribe({
      next: (data) => { this.livres = data; },
      error: () => {}
    });
  }

  chargerReservations(): void {
    this.reservationService.listerParAdherent(this.userId).subscribe({
      next: (data) => { this.reservations = data; },
      error: () => { this.reservations = []; }
    });
  }

  libelleLivre(livre: Books): string {
    const copies = livre.noOfCopies === 0
      ? 'aucun exemplaire disponible'
      : `${livre.noOfCopies} exemplaire(s)`;
    return `${livre.bookName} — ${livre.bookAuthor} (${copies})`;
  }

  reserver(): void {
    if (!this.livreIdChoisi || this.reservationEnCours) { return; }
    this.reservationEnCours = true;
    this.erreurReservation = null;
    this.succes = null;

    this.reservationService.creer(this.livreIdChoisi, this.userId).subscribe({
      next: (r) => {
        this.reservationEnCours = false;
        this.livreIdChoisi = null;
        this.jetonReinit++;
        this.succes = `Reservation #${r.id} created for « ${r.livreTitre} ».`;
        this.chargerReservations();
      },
      error: (err) => {
        this.reservationEnCours = false;
        this.erreurReservation = ReservationService.messageDe(err);
      }
    });
  }

  annuler(reservation: Reservation): void {
    this.succes = null;
    this.erreur = null;
    this.reservationService.annuler(reservation.id).subscribe({
      next: (r) => {
        this.succes = `Reservation #${r.id} cancelled.`;
        this.reservations = (this.reservations ?? []).map(x => x.id === r.id ? r : x);
      },
      error: (err) => {
        const e = ReservationService.messageDe(err);
        this.erreur = e.message;
      }
    });
  }

  libelleStatut(statut: string): string {
    const s = STATUTS.find(x => x.code === statut);
    return s ? s.libelle : statut;
  }

  classeStatut(statut: string): string {
    const s = STATUTS.find(x => x.code === statut);
    return s ? s.classe : 'badge--neutral';
  }

  estAnnulable(statut: string): boolean {
    return statut === 'EN_ATTENTE' || statut === 'DISPONIBLE';
  }
}
