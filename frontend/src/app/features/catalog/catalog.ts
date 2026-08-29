import { Component, inject, OnInit, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BookService } from '@core/services/book.service';
import { BookResponse } from '@core/models/book.models';
import { Nav } from '@shared/components/nav/nav';

@Component({
  selector: 'app-catalog',
  imports: [ReactiveFormsModule, RouterLink, Nav],
  templateUrl: './catalog.html',
  styleUrl: './catalog.scss',
})
export class Catalog implements OnInit {
  private readonly bookService = inject(BookService);
  private readonly fb = inject(NonNullableFormBuilder);

  books = signal<BookResponse[]>([]);
  loading = signal<boolean>(true);
  searching = signal<boolean>(false);
  error = signal<string>('');
  searched = signal<boolean>(false);

  searchForm = this.fb.group({ q: [''] });

  ngOnInit(): void {
    this.loadCatalog();
  }

  loadCatalog(): void {
    this.loading.set(true);
    this.error.set('');
    this.searched.set(false);
    this.bookService.search('').subscribe({
      next: (books) => {
        this.books.set(books);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set($localize`:@@catalogErrorLoad:Failed to load the catalog.`);
      },
    });
  }

  doSearch(): void {
    const q = this.searchForm.getRawValue().q.trim();
    if (!q) {
      this.loadCatalog();
      return;
    }
    this.searching.set(true);
    this.error.set('');
    this.searched.set(true);
    this.bookService.searchFull(q).subscribe({
      next: (books) => {
        this.books.set(books);
        this.searching.set(false);
      },
      error: () => {
        this.searching.set(false);
        this.error.set($localize`:@@catalogErrorSearch:Search failed. Try again.`);
      },
    });
  }
}
