import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { BookResponse } from '@core/models/book.models';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class BookService {
  private readonly http = inject(HttpClient);

  search(q: string): Observable<BookResponse[]> {
    const params = new HttpParams().set('q', q); // Codifica automáticamente caracteres especiales (títulos con espacios, acentos, &, ?...)
    return this.http.get<BookResponse[]>(`/books/search`, { params });
  }

  searchFull(q: string): Observable<BookResponse[]> {
    const params = new HttpParams().set('q', q);
    return this.http.get<BookResponse[]>(`/books/search/full`, { params });
  }

  getByIsbn(isbn: string): Observable<BookResponse> {
    return this.http.get<BookResponse>(`/books/${isbn}`);
  }
}
