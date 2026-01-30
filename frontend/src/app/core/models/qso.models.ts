export interface QsoRequest {
  theirCallsign: string;
  qsoDate: string;  // ISO date string
  timeOn: string;   // HH:mm:ss
  band: string;
  frequencyKhz?: number | null;
  mode: string;
  submode?: string | null;
  customMode?: string | null;
  rstSent?: string | null;
  rstRecv?: string | null;
  qth?: string | null;
  gridSquare?: string | null;
  notes?: string | null;
  confirmDuplicate?: boolean;
}

export interface QsoResponse {
  id: string;
  theirCallsign: string;
  qsoDate: string;
  timeOn: string;
  band: string;
  frequencyKhz?: number;
  mode: string;
  submode?: string;
  customMode?: string;
  rstSent?: string;
  rstRecv?: string;
  qth?: string;
  gridSquare?: string;
  notes?: string;
  qslStatus?: string;
  lotwStatus?: string;
  eqslStatus?: string;
  createdAt: string;
  updatedAt: string;
}

export interface DuplicateWarning {
  message: string;
  existingQsoIds: string[];
}

export interface CallsignSuggestion {
  callsign: string;
  lastKnownName?: string;
  lastKnownQth?: string;
  lastNotesSnippet?: string;
  mostCommonBand?: string;
  mostCommonMode?: string;
}

export interface ModeOption {
  label: string;
  mode: string;
  submode?: string;
  isCustom?: boolean;
}

export const MODE_OPTIONS: ModeOption[] = [
  { label: 'CW', mode: 'CW' },
  { label: 'SSB', mode: 'SSB' },
  { label: 'AM', mode: 'AM' },
  { label: 'FM', mode: 'FM' },
  { label: 'RTTY', mode: 'RTTY' },
  { label: 'PSK31', mode: 'PSK', submode: 'PSK31' },
  { label: 'FT8', mode: 'MFSK', submode: 'FT8' },
  { label: 'FT4', mode: 'MFSK', submode: 'FT4' },
  { label: 'JS8', mode: 'MFSK', submode: 'JS8' },
  { label: 'Other (Custom)', mode: 'DATA', isCustom: true }
];

export const BAND_OPTIONS = [
  '160m', '80m', '60m', '40m', '30m', '20m', '17m', '15m', '12m', '10m',
  '6m', '4m', '2m', '1.25m', '70cm', '33cm', '23cm'
];
