import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FeedPageResponse } from '@core/models/feed.models';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class FeedService {
  private readonly http = inject(HttpClient);

  getFeed(cursor?: string, limit = 10): Observable<FeedPageResponse> {
    const params: Record<string, string> = { limit: String(limit) };
    if (cursor) params['cursor'] = cursor;
    return this.http.get<FeedPageResponse>('/feed', { params });
  }
}