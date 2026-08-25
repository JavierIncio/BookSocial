import { Routes } from '@angular/router';

export const authorDetailRoutes: Routes = [
  {
    path: 'author/:authorId',
    loadComponent: () => import('./author-detail').then((m) => m.AuthorDetail),
  },
];
