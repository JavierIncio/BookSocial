import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { LoginRequest, TokenResponse } from '@core/models/auth.models';
import { BehaviorSubject, firstValueFrom, Observable, tap } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly accessTokenStore = new BehaviorSubject<string | null>(null);
  private readonly authenticated = new BehaviorSubject<boolean>(false);

  constructor(private readonly http: HttpClient) {}

  get accessToken() {
    return this.accessTokenStore.getValue();
  }

  get isAuthenticated() {
    return this.authenticated.getValue();
  }

  login(credentials: LoginRequest): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>('/auth/login', credentials)
      .pipe(tap((tokens) => this.applyToken(tokens)));
  }

  register(payload: LoginRequest): Observable<TokenResponse> {
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
    this.accessTokenStore.next(tokens.accessToken);
    this.authenticated.next(true);
  }

  clearSession(): void {
    this.accessTokenStore.next(null);
    this.authenticated.next(false);
  }
}
