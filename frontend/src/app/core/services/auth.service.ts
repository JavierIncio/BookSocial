import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { LoginRequest, RegisterRequest, TokenResponse } from '@core/models/auth.models';
import { firstValueFrom, Observable, tap } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly accessTokenStore = signal<string | null>(null);
  private readonly authenticatedStore = signal<boolean>(false);

  readonly accessToken = this.accessTokenStore.asReadonly();
  readonly isAuthenticated = this.authenticatedStore.asReadonly();

  login(credentials: LoginRequest): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>('/auth/login', credentials)
      .pipe(tap((tokens) => this.applyToken(tokens)));
  }

  register(payload: RegisterRequest): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>('/auth/register', payload)
      .pipe(tap((tokens) => this.applyToken(tokens)));
  }

  refresh(): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>('/auth/refresh', null, { withCredentials: true })
      .pipe(tap((tokens) => this.applyToken(tokens)));
  }

  logout(): Observable<void> {
    return this.http
      .post<void>('/auth/logout', null, { withCredentials: true })
      .pipe(tap(() => this.clearSession()));
  }

  userId(): number | null {
    const token = this.accessTokenStore();
    if (!token) return null;
    try {
      const payload = token.split('.')[1];
      const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/'))) as {
        uid?: number;
      };
      return typeof decoded.uid === 'number' ? decoded.uid : null;
    } catch {
      return null;
    }
  }

  applyOAuthToken(accessToken: string): void {
    this.applyToken({
      accessToken,
      refreshToken: '',
      expiresIn: 0,
      tokenType: 'Bearer',
    });
  }

  async restoreSession(): Promise<void> {
    try {
      await firstValueFrom(this.refresh());
    } catch {
      this.clearSession();
    }
  }

  private applyToken(tokens: TokenResponse): void {
    this.accessTokenStore.set(tokens.accessToken);
    this.authenticatedStore.set(true);
  }

  clearSession(): void {
    this.accessTokenStore.set(null);
    this.authenticatedStore.set(false);
  }
}
