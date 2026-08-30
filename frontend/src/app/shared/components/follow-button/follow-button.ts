import { Component, inject, input, OnInit, signal } from '@angular/core';
import { AuthService } from '@core/services/auth.service';
import { FollowService } from '@core/services/follow.service';

@Component({
  selector: 'app-follow-button',
  imports: [],
  templateUrl: './follow-button.html',
  styleUrl: './follow-button.scss',
})
export class FollowButton implements OnInit {
  private readonly followService = inject(FollowService);
  private readonly auth = inject(AuthService);

  readonly userId = input.required<number>();

  readonly busy = signal<boolean>(false);

  get isSelf(): boolean {
    return this.auth.userId() === this.userId();
  }

  get isFollowing(): boolean {
    return this.followService.isFollowing(this.userId());
  }

  ngOnInit(): void {
    this.followService.ensureLoaded();
  }

  onClick(): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.followService.toggle(this.userId()).subscribe({
      next: () => this.busy.set(false),
      error: () => this.busy.set(false),
    });
  }
}
