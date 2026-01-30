import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { StatsService } from '../../core/services/stats.service';
import { StatsResponse } from '../../core/models/stats.models';

@Component({
  selector: 'app-stats',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatInputModule,
    MatButtonModule
  ],
  templateUrl: './stats.component.html',
  styleUrls: ['./stats.component.scss']
})
export class StatsComponent implements OnInit {
  stats?: StatsResponse;
  loading = false;
  filterForm: FormGroup;

  constructor(
    private statsService: StatsService,
    private fb: FormBuilder
  ) {
    this.filterForm = this.fb.group({
      from: [null],
      to: [null]
    });
  }

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    this.loading = true;
    const filters = this.filterForm.value;

    const from = filters.from ? this.formatDate(filters.from) : undefined;
    const to = filters.to ? this.formatDate(filters.to) : undefined;

    this.statsService.getSummary(from, to).subscribe({
      next: (stats) => {
        this.stats = stats;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  clearFilters(): void {
    this.filterForm.reset();
    this.loadStats();
  }

  private formatDate(date: Date): string {
    if (!date) return '';
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  getPercentage(confirmed: number, total: number): number {
    return total > 0 ? Math.round((confirmed / total) * 100) : 0;
  }
}
