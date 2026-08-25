import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { BookService } from '@core/services/book.service';
import { ReviewService } from '@core/services/review.service';
import { ShelfService } from '@core/services/shelf.service';
import { BookResponse } from '@core/models/book.models';
import { ReviewResponse, ReviewSummaryResponse } from '@core/models/review.models';
import { ShelfResponse, ShelfStatus } from '@core/models/shelf.models';
import { Nav } from '@shared/components/nav/nav';

@Component({
  selector: 'app-book-detail',
  imports: [RouterLink, DatePipe, DecimalPipe, Nav],
  templateUrl: './book-detail.html',
  styleUrl: './book-detail.scss',
})
export class BookDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly bookService = inject(BookService);
  private readonly reviewService = inject(ReviewService);
  private readonly shelfService = inject(ShelfService);
  private readonly auth = inject(AuthService);

  readonly isAuthenticated = this.auth.isAuthenticated;

  readonly statuses: ShelfStatus[] = ['WANTS_TO_READ', 'READING', 'READ'];
  private readonly statusLabels: Record<ShelfStatus, string> = {
    WANTS_TO_READ: 'Want to read',
    READING: 'Reading',
    READ: 'Read',
  };

  book = signal<BookResponse | null>(null);
  summary = signal<ReviewSummaryResponse | null>(null);
  reviews = signal<ReviewResponse[]>([]);
  myShelfEntry = signal<ShelfResponse | null>(null);

  loading = signal<boolean>(true);
  error = signal<string>('');
  savingShelf = signal<boolean>(false);
  shelfError = signal<string>('');

  isbn = '';

  ngOnInit(): void {
    this.isbn = this.route.snapshot.paramMap.get('isbn') ?? '';
    if (!this.isbn) {
      this.error.set('ISBN not found in route.');
      this.loading.set(false);
      return;
    }
    this.loadBook();
    if (this.isAuthenticated()) {
      this.loadReviews();
      this.loadMyShelfEntry();
    }
  }

  private loadBook(): void {
    this.bookService.getByIsbn(this.isbn).subscribe({
      next: (book) => {
        this.book.set(book);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Book not found.');
      },
    });
  }

  private loadReviews(): void {
    this.reviewService.summary(this.isbn).subscribe({
      next: (summary) => this.summary.set(summary),
      error: () => this.summary.set(null),
    });
    this.reviewService.byBook(this.isbn).subscribe({
      next: (reviews) => this.reviews.set(reviews),
      error: () => this.reviews.set([]),
    });
  }

  private loadMyShelfEntry(): void {
    this.shelfService.mine().subscribe({
      next: (shelves) => {
        this.myShelfEntry.set(shelves.find((s) => s.bookIsbn === this.isbn) ?? null);
      },
      error: () => this.myShelfEntry.set(null),
    });
  }

  label(status: ShelfStatus): string {
    return this.statusLabels[status];
  }

  stars(rating: number): string {
    const rounded = Math.max(0, Math.min(5, Math.round(rating)));
    return '★'.repeat(rounded) + '☆'.repeat(5 - rounded);
  }

  addToShelf(status: ShelfStatus): void {
    this.savingShelf.set(true);
    this.shelfError.set('');
    this.shelfService.create({ bookIsbn: this.isbn, status }).subscribe({
      next: (entry) => {
        this.myShelfEntry.set(entry);
        this.savingShelf.set(false);
      },
      error: () => {
        this.savingShelf.set(false);
        this.shelfError.set('Could not update your shelf. Try again.');
      },
    });
  }

  changeStatus(status: ShelfStatus): void {
    if (this.myShelfEntry()?.status === status) return;
    this.savingShelf.set(true);
    this.shelfError.set('');
    this.shelfService.updateStatus(this.isbn, status).subscribe({
      next: (entry) => {
        this.myShelfEntry.set(entry);
        this.savingShelf.set(false);
      },
      error: () => {
        this.savingShelf.set(false);
        this.shelfError.set('Could not update status. Try again.');
      },
    });
  }

  removeFromShelf(): void {
    this.savingShelf.set(true);
    this.shelfError.set('');
    this.shelfService.remove(this.isbn).subscribe({
      next: () => {
        this.myShelfEntry.set(null);
        this.savingShelf.set(false);
      },
      error: () => {
        this.savingShelf.set(false);
        this.shelfError.set('Could not remove the book. Try again.');
      },
    });
  }
}
