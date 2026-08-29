import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';

export const authGuard = async (): Promise<boolean> => {
  const auth = inject(AuthService);
  const router = inject(Router);

  await auth.ensureSession();

  if (!auth.isAuthenticated()) {
    router.navigate(['/login']);
    return false;
  }
  return true;
};
