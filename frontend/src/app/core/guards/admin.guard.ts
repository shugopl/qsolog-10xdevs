import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { map } from 'rxjs/operators';

/**
 * Admin guard to protect routes that require ADMIN role.
 * Redirects OPERATOR users to /qso.
 * Redirects unauthenticated users to /login.
 */
export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Check authentication first
  if (!authService.isAuthenticated()) {
    router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }

  // Check ADMIN role
  return authService.currentUser$.pipe(
    map(user => {
      if (user && user.role === 'ADMIN') {
        return true;
      }
      // Redirect OPERATOR to QSO log
      router.navigate(['/qso']);
      return false;
    })
  );
};
