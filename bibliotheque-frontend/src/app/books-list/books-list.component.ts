import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Books } from '../_model/books'
import { BooksService } from '../_service/books.service';
import { ActionMenuItem } from '../_shared/action-menu/action-menu.component';

@Component({
  selector: 'app-books-list',
  templateUrl: './books-list.component.html',
  styleUrls: ['./books-list.component.css']
})
export class BooksListComponent implements OnInit {

  books: Books[] = [];
  bookToDelete: Books | null = null;

  // Pagination
  page = 1;
  pageSize = 10;

  // Menu actions
  menuItems: ActionMenuItem[] = [
    { label: 'Détails' },
    { label: 'Modifier' },
    { label: 'Supprimer', danger: true }
  ];

  constructor(private booksService: BooksService, private router: Router) {}

  ngOnInit(): void { this.getBooks(); }

  private getBooks() {
    this.booksService.getBooksList().subscribe(data => { this.books = data; });
  }

  get pagedBooks(): Books[] {
    const start = (this.page - 1) * this.pageSize;
    return this.books.slice(start, start + this.pageSize);
  }

  onPageChange(p: number) { this.page = p; }

  onMenuAction(book: Books, actionIndex: number) {
    if (actionIndex === 0) this.router.navigate(['book-details', book.bookId]);
    else if (actionIndex === 1) this.router.navigate(['update-book', book.bookId]);
    else if (actionIndex === 2) this.confirmDelete(book);
  }

  confirmDelete(book: Books) { this.bookToDelete = book; }

  deleteBook(bookId: number) {
    this.bookToDelete = null;
    this.booksService.deleteBook(bookId).subscribe(() => this.getBooks());
  }
}
