import { Routes } from '@angular/router';
import { authGuard } from '@core/guards/auth.guard';

export const userProfileRoutes: Routes = [
  {
    path: 'users/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./user-profile').then((m) => m.UserProfile),
  },
];
