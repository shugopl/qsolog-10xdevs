export interface BandStats {
  band: string;
  countAll: number;
  countConfirmed: number;
}

export interface ModeStats {
  mode: string;
  countAll: number;
  countConfirmed: number;
}

export interface DayStats {
  date: string;
  countAll: number;
  countConfirmed: number;
}

export interface Totals {
  all: number;
  confirmed: number;
}

export interface StatsResponse {
  countsByBand: BandStats[];
  countsByMode: ModeStats[];
  countsByDay: DayStats[];
  totals: Totals;
}
