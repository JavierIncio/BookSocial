import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { NotificationBell } from '@features/notifications/notification-bell/notification-bell';

@Component({
  selector: 'app-nav',
  imports: [RouterLink, NotificationBell],
  templateUrl: './nav.html',
  styleUrl: './nav.scss',
})
export class Nav {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly isAuthenticated = this.auth.isAuthenticated;

  logout(): void {
    this.auth.logout().subscribe({
      next: () => this.router.navigate(['/login']),
    });
  }
}
