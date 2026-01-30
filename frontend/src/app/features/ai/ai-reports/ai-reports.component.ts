import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatExpansionModule } from '@angular/material/expansion';
import { AiService } from '../../../core/services/ai.service';
import { AiReportResponse } from '../../../core/models/ai.models';

@Component({
  selector: 'app-ai-reports',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatExpansionModule
  ],
  templateUrl: './ai-reports.component.html',
  styleUrls: ['./ai-reports.component.scss']
})
export class AiReportsComponent implements OnInit {
  generateForm: FormGroup;
  reports: AiReportResponse[] = [];
  loading = false;
  generating = false;
  latestReport?: AiReportResponse;

  constructor(
    private aiService: AiService,
    private fb: FormBuilder
  ) {
    this.generateForm = this.fb.group({
      from: [null],
      to: [null],
      language: ['EN']
    });
  }

  ngOnInit(): void {
    this.loadReports();
  }

  loadReports(): void {
    this.loading = true;
    this.aiService.getReports().subscribe({
      next: (reports) => {
        this.reports = reports;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  generateReport(): void {
    this.generating = true;
    const formValue = this.generateForm.value;

    const from = formValue.from ? this.formatDate(formValue.from) : undefined;
    const to = formValue.to ? this.formatDate(formValue.to) : undefined;
    const lang = formValue.language || 'EN';

    this.aiService.generatePeriodReport(from, to, lang).subscribe({
      next: (report) => {
        this.latestReport = report;
        this.generating = false;
        this.loadReports(); // Refresh list
      },
      error: () => {
        this.generating = false;
      }
    });
  }

  private formatDate(date: Date): string {
    if (!date) return '';
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
