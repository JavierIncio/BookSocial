import { Routes } from '@angular/router';
import { authRoutes } from '@features/auth/routes';
import { homeRoutes } from '@features/home/routes';
import { catalogRoutes } from '@features/catalog/routes';
import { bookDetailRoutes } from '@features/book-detail/routes';
import { myShelfRoutes } from '@features/my-shelf/routes';
import { authorDetailRoutes } from '@features/author-detail/routes';
import { feedRoutes } from '@features/feed/routes';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'home' },
  ...authRoutes,
  ...homeRoutes,
  ...catalogRoutes,
  ...bookDetailRoutes,
  ...myShelfRoutes,
  ...authorDetailRoutes,
  ...feedRoutes,
  { path: '**', redirectTo: 'home' },
];
