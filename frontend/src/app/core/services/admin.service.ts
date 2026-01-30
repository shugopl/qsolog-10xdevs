import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserResponse } from '../models/auth.models';

/**
 * Admin service for user management operations.
 */
@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = environment.apiBaseUrl + '/admin';

  constructor(private http: HttpClient) {}

  /**
   * Get all users (ADMIN only).
   */
  getAllUsers(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(`${this.apiUrl}/users`);
  }
}
