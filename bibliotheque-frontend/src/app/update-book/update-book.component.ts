import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { Books } from '../_model/books';
import { BooksService } from '../_service/books.service';

@Component({
  selector: 'app-update-book',
  templateUrl: './update-book.component.html',
  styleUrls: ['./update-book.component.css']
})
export class UpdateBookComponent implements OnInit {

  bookId: number;
  book: Books = new Books();
  envoiEnCours = false;
  erreur: string | null = null;

  constructor(private booksService: BooksService, private route: ActivatedRoute, private router: Router) {}

  ngOnInit(): void {
    this.bookId = this.route.snapshot.params['bookId'];
    this.booksService.getBookById(this.bookId).subscribe(data => { this.book = data; });
  }

  onSubmit() {
    this.erreur = null;
    this.envoiEnCours = true;
    this.booksService.updateBook(this.bookId, this.book).subscribe({
      next: () => { this.envoiEnCours = false; this.router.navigate(['/books']); },
      error: (err) => { this.envoiEnCours = false; this.erreur = err?.error?.message || 'An error occurred.'; }
    });
  }

}
