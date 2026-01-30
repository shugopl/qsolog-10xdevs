import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { QsoDescriptionRequest, AiTextResponse, AiReportResponse } from '../models/ai.models';

@Injectable({
  providedIn: 'root'
})
export class AiService {
  private apiUrl = environment.apiBaseUrl + '/ai';

  constructor(private http: HttpClient) {}

  generateQsoDescription(request: QsoDescriptionRequest): Observable<AiTextResponse> {
    return this.http.post<AiTextResponse>(`${this.apiUrl}/qso-description`, request);
  }

  generatePeriodReport(from?: string, to?: string, lang: 'EN' | 'PL' = 'EN'): Observable<AiReportResponse> {
    let params = new HttpParams().set('lang', lang);
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);

    return this.http.post<AiReportResponse>(`${this.apiUrl}/period-report`, null, { params });
  }

  getReports(from?: string, to?: string): Observable<AiReportResponse[]> {
    let params = new HttpParams();
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);

    return this.http.get<AiReportResponse[]>(`${this.apiUrl}/reports`, { params });
  }

  getReport(id: string): Observable<AiReportResponse> {
    return this.http.get<AiReportResponse>(`${this.apiUrl}/reports/${id}`);
  }
}
