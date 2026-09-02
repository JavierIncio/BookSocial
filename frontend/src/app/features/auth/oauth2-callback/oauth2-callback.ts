import { Component, inject, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '@core/services/auth.service';

@Component({
  selector: 'app-oauth2-callback',
  imports: [RouterLink],
  templateUrl: './oauth2-callback.html',
  styleUrl: './oauth2-callback.scss',
})
export class Oauth2Callback implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  errorMessage = '';

  ngOnInit(): void {
    const params = new URLSearchParams(window.location.hash.replace(/^#/, ''));
    history.replaceState(null, '', new URL('oauth2/callback', document.baseURI).pathname);

    const token = params.get('access_token');
    const error = params.get('error');

    if (token) {
      this.auth.applyOAuthToken(token);
      this.router.navigate(['/home']);
      return;
    }

    this.errorMessage =
      error === 'access_denied'
        ? $localize`:@@oauthCanceled:You have canceled the Google login.`
        : $localize`:@@oauthFailed:Failed to login with Google.`;
    this.router.navigate(['/login'], { queryParams: { googleError: this.errorMessage } });
  }
}
