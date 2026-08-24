import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ShelfResponse, ShelfStatus } from '@core/models/shelf.models';

@Injectable({ providedIn: 'root' })
export class ShelfService {
  private readonly http = inject(HttpClient);

  mine(): Observable<ShelfResponse[]> {
    return this.http.get<ShelfResponse[]>(`/shelves`);
  }

  byUser(userId: number): Observable<ShelfResponse[]> {
    return this.http.get<ShelfResponse[]>(`/shelves/users/${userId}`);
  }

  create(body: { bookIsbn: string; status: ShelfStatus }): Observable<ShelfResponse> {
    return this.http.post<ShelfResponse>(`/shelves`, body);
  }

  updateStatus(isbn: string, status: ShelfStatus): Observable<ShelfResponse> {
    return this.http.put<ShelfResponse>(`/shelves/${isbn}`, { status });
  }

  remove(isbn: string): Observable<void> {
    return this.http.delete<void>(`/shelves/${isbn}`);
  }
}
