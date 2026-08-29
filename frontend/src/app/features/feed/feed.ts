import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FeedService } from '@core/services/feed.service';
import { UserService } from '@core/services/user.service';
import { AuthService } from '@core/services/auth.service';
import { FeedItemResponse } from '@core/models/feed.models';
import { Nav } from '@shared/components/nav/nav';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-feed',
  imports: [RouterLink, DatePipe, Nav],
  templateUrl: './feed.html',
  styleUrl: './feed.scss',
})
export class Feed implements OnInit {
  private readonly feedService = inject(FeedService);
  private readonly userService = inject(UserService);
  private readonly auth = inject(AuthService);

  private readonly nameCache = signal<Map<number, string>>(new Map());

  items = signal<FeedItemResponse[]>([]);
  nextCursor = signal<string | null>(null);
  loading = signal<boolean>(true);
  loadingMore = signal<boolean>(false);
  error = signal<string>('');

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.feedService.getFeed().subscribe({
      next: (page) => {
        this.items.set(page.items);
        this.nextCursor.set(page.nextCursor);
        this.loading.set(false);
        this.resolveNames(page.items);
      },
      error: () => {
        this.loading.set(false);
        this.error.set($localize`:@@feedErrorLoad:Failed to load your feed.`);
      },
    });
  }

  loadMore(): void {
    const cursor = this.nextCursor();
    if (!cursor || this.loadingMore()) return;

    this.loadingMore.set(true);
    this.feedService.getFeed(cursor).subscribe({
      next: (page) => {
        this.items.update((current) => [...current, ...page.items]);
        this.nextCursor.set(page.nextCursor);
        this.loadingMore.set(false);
        this.resolveNames(page.items);
      },
      error: () => {
        this.loadingMore.set(false);
        this.error.set($localize`:@@feedErrorMore:Failed to load more activity.`);
      },
    });
  }

  actorName(item: FeedItemResponse): string {
    const me = this.auth.userId();
    if (me !== null && item.actorId === me) {
      return $localize`:@@feedActorYou:You`;
    }
    return this.nameCache().get(item.actorId) ?? $localize`:@@feedActorUser:A reader`;
  }

  followedYou(item: FeedItemResponse): boolean {
    const me = this.auth.userId();
    return me !== null && item.payload.targetUserId === me;
  }

  shelfLabel(status?: string): string {
    const labels: Record<string, string> = {
      WANTS_TO_READ: $localize`:@@shelfStatusWantToRead:Want to read`,
      READING: $localize`:@@shelfStatusReading:Reading`,
      READ: $localize`:@@shelfStatusRead:Read`,
    };
    return (status && labels[status]) || status || '';
  }

  private resolveNames(items: FeedItemResponse[]): void {
    const me = this.auth.userId();
    const ids = [
      ...new Set(
        items
          .map((i) => i.actorId)
          .filter((id) => me === null || id !== me),
      ),
    ];
    ids.forEach((id) => this.resolveName(id));
  }

  private async resolveName(userId: number): Promise<void> {
    if (this.nameCache().has(userId)) return;
    try {
      const profile = await firstValueFrom(this.userService.profile(userId));
      const name = profile.displayName || profile.email;
      this.nameCache.update((m) => new Map(m).set(userId, name));
    } catch {
      this.nameCache.update((m) => new Map(m).set(userId, $localize`:@@feedActorUser:A reader`));
    }
  }
}