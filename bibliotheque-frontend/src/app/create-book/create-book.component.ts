import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Books } from '../_model/books';
import { BooksService } from '../_service/books.service';

@Component({
  selector: 'app-create-book',
  templateUrl: './create-book.component.html',
  styleUrls: ['./create-book.component.css']
})
export class CreateBookComponent implements OnInit {

  book: Books = new Books();
  envoiEnCours = false;
  erreur: string | null = null;

  constructor(private booksService: BooksService, private router: Router) {}

  ngOnInit(): void {}

  onSubmit() {
    this.erreur = null;
    this.envoiEnCours = true;
    this.booksService.createBook(this.book).subscribe({
      next: () => { this.envoiEnCours = false; this.router.navigate(['/books']); },
      error: (err) => { this.envoiEnCours = false; this.erreur = err?.error?.message || 'Une erreur est survenue.'; }
    });
  }

}
