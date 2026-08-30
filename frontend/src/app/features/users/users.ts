import { Component, inject, OnInit, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProfileResponse } from '@core/models/user.models';
import { FollowService } from '@core/services/follow.service';
import { Nav } from '@shared/components/nav/nav';
import { FollowButton } from '@shared/components/follow-button/follow-button';

@Component({
  selector: 'app-users',
  imports: [ReactiveFormsModule, RouterLink, Nav, FollowButton],
  templateUrl: './users.html',
  styleUrl: './users.scss',
})
export class Users implements OnInit {
  private readonly followService = inject(FollowService);
  private readonly fb = inject(NonNullableFormBuilder);

  profiles = signal<ProfileResponse[]>([]);
  loading = signal<boolean>(true);
  searching = signal<boolean>(false);
  error = signal<string>('');

  searchForm = this.fb.group({ q: [''] });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.followService.search('').subscribe({
      next: (profiles) => {
        this.profiles.set(profiles);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set($localize`:@@usersErrorLoad:Failed to load users.`);
      },
    });
  }

  doSearch(): void {
    const q = this.searchForm.getRawValue().q.trim();
    if (!q) {
      this.load();
      return;
    }
    this.searching.set(true);
    this.error.set('');
    this.followService.search(q).subscribe({
      next: (profiles) => {
        this.profiles.set(profiles);
        this.searching.set(false);
      },
      error: () => {
        this.searching.set(false);
        this.error.set($localize`:@@usersErrorSearch:Search failed. Try again.`);
      },
    });
  }
}
