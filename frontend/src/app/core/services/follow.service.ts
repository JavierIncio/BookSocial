import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { AuthService } from '@core/services/auth.service';
import { FollowResponse, ProfileResponse } from '@core/models/user.models';
import { Observable, tap } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class FollowService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  private readonly followingStore = signal<Set<number>>(new Set());
  private loaded = false;

  readonly followingIds = this.followingStore.asReadonly();

  isFollowing(userId: number): boolean {
    return this.followingStore().has(userId);
  }

  ensureLoaded(): void {
    if (this.loaded) return;
    this.loaded = true;
    this.reloadFollowing();
  }

  reloadFollowing(): void {
    const me = this.auth.userId();
    if (me === null) return;
    this.http.get<FollowResponse[]>(`/follows/${me}/following`).subscribe((follows) => {
      this.followingStore.set(new Set(follows.map((f) => Number(f.followeeId))));
    });
  }

  follow(targetUserId: number): Observable<FollowResponse> {
    return this.http
      .post<FollowResponse>(`/follows/${targetUserId}`, null)
      .pipe(tap(() => this.markFollowing(targetUserId, true)));
  }

  unfollow(targetUserId: number): Observable<void> {
    return this.http
      .delete<void>(`/follows/${targetUserId}`)
      .pipe(tap(() => this.markFollowing(targetUserId, false)));
  }

  toggle(targetUserId: number): Observable<unknown> {
    return this.isFollowing(targetUserId)
      ? this.unfollow(targetUserId)
      : this.follow(targetUserId);
  }

  followers(userId: number): Observable<FollowResponse[]> {
    return this.http.get<FollowResponse[]>(`/follows/${userId}/followers`);
  }

  following(userId: number): Observable<FollowResponse[]> {
    return this.http.get<FollowResponse[]>(`/follows/${userId}/following`);
  }

  search(query: string): Observable<ProfileResponse[]> {
    return this.http.get<ProfileResponse[]>('/profiles/search', { params: { q: query } });
  }

  private markFollowing(targetUserId: number, value: boolean): void {
    const next = new Set(this.followingStore());
    if (value) next.add(targetUserId);
    else next.delete(targetUserId);
    this.followingStore.set(next);
  }
}
