import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { StatsResponse } from '../models/stats.models';

@Injectable({
  providedIn: 'root'
})
export class StatsService {
  private apiUrl = environment.apiBaseUrl + '/stats';

  constructor(private http: HttpClient) {}

  getSummary(from?: string, to?: string): Observable<StatsResponse> {
    let params = new HttpParams();
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);

    return this.http.get<StatsResponse>(`${this.apiUrl}/summary`, { params });
  }
}
