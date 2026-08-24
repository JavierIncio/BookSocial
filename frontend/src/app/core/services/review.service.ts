import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { ReviewResponse, ReviewSummaryResponse } from '@core/models/review.models';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ReviewService {
  private readonly http = inject(HttpClient);

  byBook(isbn: string): Observable<ReviewResponse[]> {
    return this.http.get<ReviewResponse[]>(`/reviews/books/${isbn}`);
  }

  summary(isbn: string): Observable<ReviewSummaryResponse> {
    return this.http.get<ReviewSummaryResponse>(`/reviews/books/${isbn}/summary`);
  }

  mine(): Observable<ReviewResponse[]> {
    return this.http.get<ReviewResponse[]>(`/reviews/me`);
  }

  create(
    isbn: string,
    body: { rating: number; comment: string | null },
  ): Observable<ReviewResponse> {
    return this.http.post<ReviewResponse>(`/reviews/${isbn}`, body);
  }

  update(
    isbn: string,
    body: { rating: number; comment: string | null },
  ): Observable<ReviewResponse> {
    return this.http.put<ReviewResponse>(`/reviews/${isbn}`, body);
  }
}
