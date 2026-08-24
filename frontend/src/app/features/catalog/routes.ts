import { Routes } from '@angular/router';

export const catalogRoutes: Routes = [
  {
    path: 'catalog',
    loadComponent: () => import('./catalog').then((m) => m.Catalog),
  },
];
