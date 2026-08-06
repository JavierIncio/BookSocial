import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { UserService } from '@core/services/user.service';
import { UserResponse } from '@core/models/user.models';
import { InitialsPipe } from '@shared/pipes/initials.pipe';
import { CapitalizePipe } from '@shared/pipes/capitalize.pipe';

@Component({
  selector: 'app-home',
  imports: [InitialsPipe, CapitalizePipe],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly userService = inject(UserService);

  user: UserResponse | null = null;
  loading = true;
  error = '';

  ngOnInit(): void {
    this.userService.me().subscribe({
      next: (user) => {
        this.user = user;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = 'Failed to load your profile.';
      },
    });
  }

  logout(): void {
    this.auth.logout().subscribe({
      next: () => this.router.navigate(['/login']),
    });
  }
}
