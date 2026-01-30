import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatCardModule } from '@angular/material/card';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { QsoService } from '../../../core/services/qso.service';
import { QsoResponse, BAND_OPTIONS } from '../../../core/models/qso.models';

@Component({
  selector: 'app-qso-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatCardModule,
    MatDialogModule
  ],
  templateUrl: './qso-list.component.html',
  styleUrls: ['./qso-list.component.scss']
})
export class QsoListComponent implements OnInit {
  displayedColumns: string[] = ['theirCallsign', 'qsoDate', 'timeOn', 'band', 'mode', 'rstSent', 'rstRecv', 'actions'];
  dataSource: QsoResponse[] = [];
  totalRecords = 0;
  pageSize = 20;
  pageIndex = 0;

  filterForm: FormGroup;
  bandOptions = BAND_OPTIONS;

  loading = false;

  constructor(
    private qsoService: QsoService,
    private fb: FormBuilder,
    private router: Router,
    private dialog: MatDialog
  ) {
    this.filterForm = this.fb.group({
      callsign: [''],
      band: [''],
      from: [null],
      to: [null]
    });
  }

  ngOnInit(): void {
    this.loadQsos();

    // Subscribe to filter changes
    this.filterForm.valueChanges.subscribe(() => {
      this.pageIndex = 0;
      this.loadQsos();
    });
  }

  loadQsos(): void {
    this.loading = true;
    const filters = this.filterForm.value;

    const params: any = {
      page: this.pageIndex,
      size: this.pageSize
    };

    if (filters.callsign) params.callsign = filters.callsign;
    if (filters.band) params.band = filters.band;
    if (filters.from) params.from = this.formatDate(filters.from);
    if (filters.to) params.to = this.formatDate(filters.to);

    this.qsoService.getQsos(params).subscribe({
      next: (qsos) => {
        this.dataSource = qsos;
        this.totalRecords = qsos.length; // Note: Backend doesn't return total count in current implementation
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadQsos();
  }

  editQso(qso: QsoResponse): void {
    this.router.navigate(['/qso', qso.id, 'edit']);
  }

  deleteQso(qso: QsoResponse): void {
    if (confirm(`Delete QSO with ${qso.theirCallsign}?`)) {
      this.qsoService.deleteQso(qso.id).subscribe({
        next: () => {
          this.loadQsos();
        }
      });
    }
  }

  clearFilters(): void {
    this.filterForm.reset();
  }

  private formatDate(date: Date): string {
    if (!date) return '';
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
