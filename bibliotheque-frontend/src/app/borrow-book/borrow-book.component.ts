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

  books: Books[] = [];
  borrow: Borrow = new Borrow();
  borrowingId: number | null = null;
  succes: string | null = null;
  erreur: string | null = null;
  userId = this.userAuthService.getUserId();

  // Pagination
  page = 1;
  pageSize = 10;

  constructor(private booksService: BooksService, private userAuthService: UserAuthService, private borrowService: BorrowService) {}

  ngOnInit(): void { this.getBooks(); }

  private getBooks() {
    this.booksService.getBooksList().subscribe(data => { this.books = data; });
  }

  get pagedBooks(): Books[] {
    const start = (this.page - 1) * this.pageSize;
    return this.books.slice(start, start + this.pageSize);
  }

  onPageChange(p: number) { this.page = p; }

  borrowBook(bookId: number) {
    this.succes = null;
    this.erreur = null;
    this.borrowingId = bookId;
    this.borrow.bookId = bookId;
    this.borrow.userId = this.userId;
    this.borrowService.borrowBook(this.borrow).subscribe({
      next: () => {
        this.borrowingId = null;
        this.succes = 'Livre emprunté avec succès !';
        this.getBooks();
        this.autoDismiss();
      },
      error: (err) => {
        this.borrowingId = null;
        this.erreur = err?.error?.message || 'Échec de l\'emprunt du livre.';
        this.autoDismiss();
      }
    });
  }

  // Auto-dismiss alerts after 5s
  private autoDismiss() {
    setTimeout(() => { this.succes = null; this.erreur = null; }, 5000);
  }
}
