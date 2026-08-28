import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Books } from '../_model/books'
import { BooksService } from '../_service/books.service';

@Component({
  selector: 'app-books-list',
  templateUrl: './books-list.component.html',
  styleUrls: ['./books-list.component.css']
})
export class BooksListComponent implements OnInit {

  books: Books[];
  bookToDelete: Books | null = null;

  constructor(private booksService: BooksService, private router: Router) {}

  ngOnInit(): void { this.getBooks(); }

  private getBooks() {
    this.booksService.getBooksList().subscribe(data => { this.books = data; });
  }

  confirmDelete(book: Books) { this.bookToDelete = book; }

  deleteBook(bookId: number) {
    this.bookToDelete = null;
    this.booksService.deleteBook(bookId).subscribe(() => this.getBooks());
  }

  updateBook(bookId: number) { this.router.navigate(['update-book', bookId]); }
  bookDetails(bookId: number) { this.router.navigate(['book-details', bookId]); }

}
