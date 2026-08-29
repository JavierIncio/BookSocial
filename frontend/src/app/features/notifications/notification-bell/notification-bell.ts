import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { NotificationService } from '@core/services/notification.service';
import { NotificationRealtimeService } from '@core/services/notification.realtime.service';
import { UserService } from '@core/services/user.service';
import { NotificationResponse } from '@core/models/notification.models';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-notification-bell',
  templateUrl: './notification-bell.html',
  styleUrl: './notification-bell.scss',
})
export class NotificationBell implements OnInit, OnDestroy {
  private readonly notifications = inject(NotificationService);
  private readonly realtime = inject(NotificationRealtimeService);
  private readonly users = inject(UserService);

  private readonly nameCache = signal<Map<number, string>>(new Map());

  open = signal<boolean>(false);
  loading = signal<boolean>(true);
  error = signal<string>('');
  unread = signal<number>(0);
  items = signal<NotificationResponse[]>([]);

  ngOnInit(): void {
    this.load();
    this.realtime.connect((notification) => this.onIncoming(notification));
  }

  ngOnDestroy(): void {
    this.realtime.disconnect();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.notifications.unreadCount().subscribe({
      next: (count) => this.unread.set(count),
      error: () => this.unread.set(0),
    });
    this.notifications.list().subscribe({
      next: (list) => {
        this.items.set(list);
        this.loading.set(false);
        this.resolveNames(list);
      },
      error: () => {
        this.loading.set(false);
        this.error.set($localize`:@@notifErrorLoad:Failed to load notifications.`);
      },
    });
  }

  private onIncoming(notification: NotificationResponse): void {
    this.items.update((current) => [
      notification,
      ...current.filter((existing) => existing.id !== notification.id),
    ]);
    this.unread.update((count) => count + 1);
    this.resolveNames([notification]);
  }

  toggle(): void {
    this.open.update((value) => !value);
  }

  markAllAsRead(): void {
    this.notifications.markAllAsRead().subscribe({
      error: () => this.unread.set(0),
    });
    this.unread.set(0);
    this.items.update((list) => list.map((n) => ({ ...n, read: true })));
  }

  actorName(notification: NotificationResponse): string {
    const followerId = notification.payload?.followerId;
    if (followerId === undefined) return $localize`:@@notifActorUser:Another reader`;
    return this.nameCache().get(followerId) ?? $localize`:@@notifActorUser:Another reader`;
  }

  private resolveNames(items: NotificationResponse[]): void {
    const ids = [
      ...new Set(
        items
          .map((n) => n.payload?.followerId)
          .filter((id): id is number => id !== undefined),
      ),
    ];
    ids.forEach((id) => this.resolveName(id));
  }

  private async resolveName(userId: number): Promise<void> {
    if (this.nameCache().has(userId)) return;
    try {
      const profile = await firstValueFrom(this.users.profile(userId));
      const name = profile.displayName || profile.email;
      this.nameCache.update((m) => new Map(m).set(userId, name));
    } catch {
      this.nameCache.update((m) =>
        new Map(m).set(userId, $localize`:@@notifActorUser:Another reader`),
      );
    }
  }
}