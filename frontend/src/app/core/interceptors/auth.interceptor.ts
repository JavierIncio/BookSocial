import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '@core/services/auth.service';

const AUTH_ENDPOINTS = [
  '/auth/login',
  '/auth/register',
  '/auth/refresh',
  '/auth/logout',
  '/auth/forgot-password',
  '/auth/reset-password',
];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);

  if (AUTH_ENDPOINTS.some((endpoint) => req.url.startsWith(endpoint))) {
    return next(req);
  }

  const token = auth.accessToken();
  const authorized = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(authorized).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401 || !auth.isAuthenticated()) {
        return throwError(() => error);
      }

      return auth.refresh().pipe(
        catchError(() => {
          auth.clearSession();
          return throwError(() => error);
        }),
        switchMap(() => {
          const freshToken = auth.accessToken();
          return next(
            freshToken ? req.clone({ setHeaders: { Authorization: `Bearer ${freshToken}` } }) : req,
          );
        }),
      );
    }),
  );
};
