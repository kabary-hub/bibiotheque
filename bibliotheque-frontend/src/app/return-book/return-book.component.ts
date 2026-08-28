import { Component, OnInit } from '@angular/core';
import { Books } from '../_model/books';
import { Borrow } from '../_model/borrow';
import { BooksService } from '../_service/books.service';
import { BorrowService } from '../_service/borrow.service';
import { UserAuthService } from '../_service/user-auth.service';

@Component({
  selector: 'app-return-book',
  templateUrl: './return-book.component.html',
  styleUrls: ['./return-book.component.css']
})
export class ReturnBookComponent implements OnInit {

  books: Books[];
  borrow: Borrow[];

  constructor(
    private borrowService: BorrowService,
    private booksService: BooksService,
    private userAuthService: UserAuthService
  ) { }

  userId = this.userAuthService.getUserId();

  ngOnInit(): void {
    this.getBooks();
    this.getBooksByUser();
  }

  private getBooks() {
    this.booksService.getBooksList().subscribe(data => {
      this.books = data;
    });
  }

  private getBooksByUser() {
    this.borrowService.getBooksBorrowedByUser(this.userId).subscribe(data => {
      this.borrow = data;
    });
  }

  brw: Borrow = new Borrow();

  public returnBook(borrowId: number) {
    this.brw.borrowId = borrowId;
    this.borrowService.returnBook(this.brw).subscribe({
      next: () => this.getBooksByUser(),
      error: err => console.error(err)
    });
  }

  public isOverdue(b: Borrow): boolean {
    if (b.returnDate) { return false; }
    return b.dueDate ? new Date(b.dueDate) < new Date() : false;
  }
}
