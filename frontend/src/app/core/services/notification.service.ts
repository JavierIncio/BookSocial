import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { NotificationResponse } from '@core/models/notification.models';
import { Observable, map } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);

  list(): Observable<NotificationResponse[]> {
    return this.http.get<NotificationResponse[]>('/notifications');
  }

  unreadCount(): Observable<number> {
    return this.http.get<{ count: number }>('/notifications/unread-count').pipe(map((r) => r.count));
  }

  markAllAsRead(): Observable<void> {
    return this.http.post<void>('/notifications/read', null);
  }
}