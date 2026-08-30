import { Routes } from '@angular/router';
import { authGuard } from '@core/guards/auth.guard';

export const usersRoutes: Routes = [
  {
    path: 'users',
    canActivate: [authGuard],
    loadComponent: () => import('./users').then((m) => m.Users),
  },
];
