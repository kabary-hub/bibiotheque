import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';

@Component({
  selector: 'app-pagination',
  templateUrl: './pagination.component.html',
  styleUrls: ['./pagination.component.css']
})
export class PaginationComponent implements OnChanges {

  @Input() totalItems = 0;
  @Input() pageSize = 10;
  @Input() currentPage = 1;
  @Output() pageChange = new EventEmitter<number>();

  totalPages = 0;
  pages: (number | string)[] = [];

  ngOnChanges(changes: SimpleChanges): void {
    this.totalPages = Math.ceil(this.totalItems / this.pageSize);
    if (this.currentPage > this.totalPages) {
      this.currentPage = Math.max(1, this.totalPages);
    }
    this.buildPages();
  }

  private buildPages(): void {
    const p: (number | string)[] = [];
    const total = this.totalPages;
    const curr = this.currentPage;

    if (total <= 7) {
      for (let i = 1; i <= total; i++) p.push(i);
    } else {
      p.push(1);
      if (curr > 3) p.push('...');
      const start = Math.max(2, curr - 1);
      const end = Math.min(total - 1, curr + 1);
      for (let i = start; i <= end; i++) p.push(i);
      if (curr < total - 2) p.push('...');
      p.push(total);
    }
    this.pages = p;
  }

  goTo(page: number | string) {
    if (typeof page !== 'number') return;
    if (page < 1 || page > this.totalPages || page === this.currentPage) return;
    this.currentPage = page;
    this.buildPages();
    this.pageChange.emit(page);
  }

  prev() { this.goTo(this.currentPage - 1); }
  next() { this.goTo(this.currentPage + 1); }
}
