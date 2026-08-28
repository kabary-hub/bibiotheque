import { Component, OnInit } from '@angular/core';
import { Books } from '../_model/books';
import { Borrow } from '../_model/borrow';
import { BooksService } from '../_service/books.service';
import { BorrowService } from '../_service/borrow.service';
import { UserAuthService } from '../_service/user-auth.service';

@Component({
  selector: 'app-borrow-book',
  templateUrl: './borrow-book.component.html',
  styleUrls: ['./borrow-book.component.css']
})
export class BorrowBookComponent implements OnInit {

  books: Books[];
  borrow: Borrow = new Borrow();
  borrowingId: number | null = null;
  succes: string | null = null;
  erreur: string | null = null;
  userId = this.userAuthService.getUserId();

  constructor(private booksService: BooksService, private userAuthService: UserAuthService, private borrowService: BorrowService) {}

  ngOnInit(): void { this.getBooks(); }

  private getBooks() {
    this.booksService.getBooksList().subscribe(data => { this.books = data; });
  }

  borrowBook(bookId: number) {
    this.succes = null;
    this.erreur = null;
    this.borrowingId = bookId;
    this.borrow.bookId = bookId;
    this.borrow.userId = this.userId;
    this.borrowService.borrowBook(this.borrow).subscribe({
      next: () => {
        this.borrowingId = null;
        this.succes = 'Book borrowed successfully!';
        this.getBooks();
      },
      error: (err) => {
        this.borrowingId = null;
        this.erreur = err?.error?.message || 'Failed to borrow book.';
      }
    });
  }
}
