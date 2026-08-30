import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { UserService } from '@core/services/user.service';
import { FollowService } from '@core/services/follow.service';
import { FollowResponse, ProfileResponse } from '@core/models/user.models';
import { Nav } from '@shared/components/nav/nav';
import { FollowButton } from '@shared/components/follow-button/follow-button';
import { firstValueFrom } from 'rxjs';

interface ListedUser {
  id: number;
  name: string;
}

@Component({
  selector: 'app-user-profile',
  imports: [RouterLink, Nav, FollowButton],
  templateUrl: './user-profile.html',
  styleUrl: './user-profile.scss',
})
export class UserProfile implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly userService = inject(UserService);
  private readonly followService = inject(FollowService);
  private readonly auth = inject(AuthService);

  userId = 0;

  profile = signal<ProfileResponse | null>(null);
  followers = signal<ListedUser[]>([]);
  following = signal<ListedUser[]>([]);
  activeTab = signal<'followers' | 'following'>('followers');

  loading = signal<boolean>(true);
  loadingList = signal<boolean>(false);
  error = signal<string>('');

  get isSelf(): boolean {
    return this.auth.userId() === this.userId;
  }

  ngOnInit(): void {
    const raw = this.route.snapshot.paramMap.get('id');
    const id = raw ? Number(raw) : NaN;
    if (!Number.isFinite(id) || id <= 0) {
      this.error.set($localize`:@@profileErrorId:User not found.`);
      this.loading.set(false);
      return;
    }
    this.userId = id;
    this.followService.ensureLoaded();
    this.loadProfile();
  }

  private loadProfile(): void {
    this.userService.profile(this.userId).subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.loading.set(false);
        this.loadList('followers');
      },
      error: () => {
        this.loading.set(false);
        this.error.set($localize`:@@profileErrorNotFound:User not found.`);
      },
    });
  }

  switchTab(tab: 'followers' | 'following'): void {
    this.activeTab.set(tab);
    this.loadList(tab);
  }

  private loadList(tab: 'followers' | 'following'): void {
    this.loadingList.set(true);
    const request =
      tab === 'followers'
        ? this.followService.followers(this.userId)
        : this.followService.following(this.userId);

    request.subscribe({
      next: (items: FollowResponse[]) => {
        this.resolveUsers(items, tab);
      },
      error: () => {
        this.loadingList.set(false);
      },
    });
  }

  private async resolveUsers(items: FollowResponse[], tab: 'followers' | 'following'): Promise<void> {
    const ids = items.map((f) => Number(tab === 'followers' ? f.followerId : f.followeeId));
    const users = await Promise.all(
      ids.map(async (id) => {
        try {
          const p = await firstValueFrom(this.userService.profile(id));
          return { id, name: p.displayName || p.email };
        } catch {
          return { id, name: $localize`:@@profileActorUser:A reader` };
        }
      }),
    );
    if (tab === 'followers') this.followers.set(users);
    else this.following.set(users);
    this.loadingList.set(false);
  }
}
