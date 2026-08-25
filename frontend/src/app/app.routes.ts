import { Routes } from '@angular/router';
import { authRoutes } from '@features/auth/routes';
import { homeRoutes } from '@features/home/routes';
import { catalogRoutes } from '@features/catalog/routes';
import { bookDetailRoutes } from '@features/book-detail/routes';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'home' },
  ...authRoutes,
  ...homeRoutes,
  ...catalogRoutes,
  ...bookDetailRoutes,
  { path: '**', redirectTo: 'home' },
];
