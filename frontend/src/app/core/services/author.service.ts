import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { AuthorResponse, WorksResponse } from '@core/models/author.models';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AuthorService {
  private readonly http = inject(HttpClient);

  search(q: string): Observable<AuthorResponse[]> {
    const params = new HttpParams().set('q', q);
    return this.http.get<AuthorResponse[]>(`/authors/search`, { params });
  }

  detail(openLibraryId: string): Observable<AuthorResponse> {
    return this.http.get<AuthorResponse>(`/authors/${openLibraryId}`);
  }

  works(openLibraryId: string): Observable<WorksResponse> {
    return this.http.get<WorksResponse>(`/authors/${openLibraryId}/works`);
  }
}
