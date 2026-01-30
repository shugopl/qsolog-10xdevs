import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { QsoRequest, QsoResponse, CallsignSuggestion } from '../models/qso.models';

@Injectable({
  providedIn: 'root'
})
export class QsoService {
  private apiUrl = environment.apiBaseUrl + '/qso';
  private suggestionsUrl = environment.apiBaseUrl + '/suggestions';

  constructor(private http: HttpClient) {}

  getQsos(params: {
    callsign?: string;
    band?: string;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
  }): Observable<QsoResponse[]> {
    let httpParams = new HttpParams();
    if (params.callsign) httpParams = httpParams.set('callsign', params.callsign);
    if (params.band) httpParams = httpParams.set('band', params.band);
    if (params.from) httpParams = httpParams.set('from', params.from);
    if (params.to) httpParams = httpParams.set('to', params.to);
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size.toString());

    return this.http.get<QsoResponse[]>(this.apiUrl, { params: httpParams });
  }

  getQso(id: string): Observable<QsoResponse> {
    return this.http.get<QsoResponse>(`${this.apiUrl}/${id}`);
  }

  createQso(request: QsoRequest): Observable<QsoResponse> {
    return this.http.post<QsoResponse>(this.apiUrl, request);
  }

  updateQso(id: string, request: QsoRequest): Observable<QsoResponse> {
    return this.http.put<QsoResponse>(`${this.apiUrl}/${id}`, request);
  }

  deleteQso(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getCallsignSuggestions(callsign: string): Observable<CallsignSuggestion> {
    return this.http.get<CallsignSuggestion>(`${this.suggestionsUrl}/callsign/${callsign}`);
  }
}
