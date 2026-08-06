import { Routes } from '@angular/router';
import { authRoutes } from '@features/auth/routes';
import { homeRoutes } from '@features/home/routes';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'home' },
  ...authRoutes,
  ...homeRoutes,
  { path: '**', redirectTo: 'home' },
];
