import { Routes } from '@angular/router';

export const bookDetailRoutes: Routes = [
  {
    path: 'book/:isbn',
    loadComponent: () => import('./book-detail').then((m) => m.BookDetail),
  },
];
