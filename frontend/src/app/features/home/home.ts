import { Component, inject, OnInit, signal } from '@angular/core';
import { UserService } from '@core/services/user.service';
import { UserResponse } from '@core/models/user.models';
import { InitialsPipe } from '@shared/pipes/initials.pipe';
import { CapitalizePipe } from '@shared/pipes/capitalize.pipe';
import { Nav } from '@shared/components/nav/nav';

@Component({
  selector: 'app-home',
  imports: [InitialsPipe, CapitalizePipe, Nav],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home implements OnInit {
  private readonly userService = inject(UserService);

  user = signal<UserResponse | null>(null);
  loading = signal<boolean>(true);
  error = signal<string>('');

  ngOnInit(): void {
    this.userService.me().subscribe({
      next: (user) => {
        this.user.set(user);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set($localize`:@@homeErrorLoad:Failed to load your profile.`);
      },
    });
  }
}
