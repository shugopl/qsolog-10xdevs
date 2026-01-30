export interface QsoDescriptionRequest {
  theirCallsign: string;
  qsoDate: string;
  timeOn: string;
  band: string;
  mode: string;
  rstSent?: string;
  rstRecv?: string;
  qth?: string;
  notes?: string;
  language: 'EN' | 'PL';
}

export interface AiTextResponse {
  language: string;
  text: string;
}

export interface AiReportResponse {
  id: string;
  dateFrom?: string;
  dateTo?: string;
  language: string;
  content: string;
  createdAt: string;
}
