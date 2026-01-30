import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { tap, catchError, map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { LoginRequest, RegisterRequest, LoginResponse, UserResponse } from '../models/auth.models';

/**
 * Authentication service.
 *
 * Token Storage Strategy:
 * - Primary: In-memory (best security, lost on refresh)
 * - Fallback: sessionStorage (survives refresh, cleared on tab close)
 *
 * Tradeoffs:
 * - In-memory: Most secure (no XSS vulnerability), but lost on refresh
 * - sessionStorage: Survives refresh, but vulnerable to XSS attacks
 * - localStorage: Persists across sessions, most vulnerable to XSS
 *
 * We use sessionStorage as fallback for better UX with short-lived tokens.
 * For production with long-lived tokens, consider httpOnly cookies.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = environment.apiBaseUrl + '/auth';
  private tokenSubject = new BehaviorSubject<string | null>(this.getStoredToken());
  public token$ = this.tokenSubject.asObservable();

  private currentUserSubject = new BehaviorSubject<UserResponse | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  // In-memory token storage (primary)
  private inMemoryToken: string | null = null;

  constructor(private http: HttpClient) {
    // Try to restore user if token exists
    const token = this.getStoredToken();
    if (token) {
      this.loadCurrentUser().subscribe();
    }
  }

  /**
   * Register a new user.
   */
  register(request: RegisterRequest): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/register`, request);
  }

  /**
   * Login and store token.
   */
  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, request).pipe(
      tap(response => {
        this.setToken(response.accessToken);
        this.loadCurrentUser().subscribe();
      })
    );
  }

  /**
   * Logout and clear token.
   */
  logout(): void {
    this.clearToken();
    this.currentUserSubject.next(null);
  }

  /**
   * Get current user information.
   */
  getCurrentUser(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/me`);
  }

  /**
   * Load current user and update subject.
   */
  private loadCurrentUser(): Observable<UserResponse | null> {
    return this.getCurrentUser().pipe(
      tap(user => this.currentUserSubject.next(user)),
      catchError(() => {
        this.clearToken();
        return of(null);
      })
    );
  }

  /**
   * Check if user is authenticated.
   */
  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  /**
   * Get current token.
   */
  getToken(): string | null {
    // Try in-memory first, then fallback to sessionStorage
    return this.inMemoryToken || this.getStoredToken();
  }

  /**
   * Set token in both in-memory and sessionStorage.
   */
  private setToken(token: string): void {
    this.inMemoryToken = token;
    sessionStorage.setItem('access_token', token);
    this.tokenSubject.next(token);
  }

  /**
   * Get token from sessionStorage.
   */
  private getStoredToken(): string | null {
    return sessionStorage.getItem('access_token');
  }

  /**
   * Clear token from all storages.
   */
  private clearToken(): void {
    this.inMemoryToken = null;
    sessionStorage.removeItem('access_token');
    this.tokenSubject.next(null);
  }
}
