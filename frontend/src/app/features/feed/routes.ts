import { Routes } from '@angular/router';
import { authGuard } from '@core/guards/auth.guard';

export const feedRoutes: Routes = [
  {
    path: 'feed',
    canActivate: [authGuard],
    loadComponent: () => import('./feed').then((m) => m.Feed),
  },
];