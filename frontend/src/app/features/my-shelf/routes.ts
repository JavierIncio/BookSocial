import { Routes } from '@angular/router';
import { authGuard } from '@core/guards/auth.guard';

export const myShelfRoutes: Routes = [
  {
    path: 'shelf',
    canActivate: [authGuard],
    loadComponent: () => import('./my-shelf').then((m) => m.MyShelf),
  },
];
