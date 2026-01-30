import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';
import { QsoService } from '../../../core/services/qso.service';
import { AiService } from '../../../core/services/ai.service';
import { QsoRequest, QsoResponse, MODE_OPTIONS, BAND_OPTIONS, ModeOption } from '../../../core/models/qso.models';

@Component({
  selector: 'app-qso-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatSnackBarModule
  ],
  templateUrl: './qso-form.component.html',
  styleUrls: ['./qso-form.component.scss']
})
export class QsoFormComponent implements OnInit {
  qsoForm: FormGroup;
  isEditMode = false;
  qsoId?: string;
  loading = false;
  savingWithConfirmDuplicate = false;

  modeOptions = MODE_OPTIONS;
  bandOptions = BAND_OPTIONS;
  selectedModeOption?: ModeOption;
  showCustomModeField = false;

  aiGenerating = false;
  aiLanguage: 'EN' | 'PL' = 'EN';
  generatedText = '';

  constructor(
    private fb: FormBuilder,
    private qsoService: QsoService,
    private aiService: AiService,
    private router: Router,
    private route: ActivatedRoute,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {
    this.qsoForm = this.fb.group({
      theirCallsign: ['', Validators.required],
      qsoDate: [new Date(), Validators.required],
      timeOn: ['', Validators.required],
      band: ['', Validators.required],
      frequencyKhz: [null],
      modeSelection: ['', Validators.required],
      customMode: [''],
      rstSent: ['59'],
      rstRecv: ['59'],
      qth: [''],
      gridSquare: [''],
      notes: ['']
    });
  }

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      if (params['id']) {
        this.isEditMode = true;
        this.qsoId = params['id'];
        if (this.qsoId) {
          this.loadQso(this.qsoId);
        }
      }
    });

    // Watch mode selection changes
    this.qsoForm.get('modeSelection')?.valueChanges.subscribe(value => {
      this.onModeSelectionChange(value);
    });

    // Watch callsign changes for suggestions
    this.qsoForm.get('theirCallsign')?.valueChanges.subscribe(callsign => {
      if (callsign && callsign.length >= 3 && !this.isEditMode) {
        this.loadSuggestions(callsign);
      }
    });
  }

  loadQso(id: string): void {
    this.loading = true;
    this.qsoService.getQso(id).subscribe({
      next: (qso) => {
        this.populateForm(qso);
        this.loading = false;
      },
      error: () => {
        this.snackBar.open('Failed to load QSO', 'Close', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  populateForm(qso: QsoResponse): void {
    // Find matching mode option
    let modeSelection = '';
    if (qso.customMode) {
      modeSelection = 'Other (Custom)';
      this.showCustomModeField = true;
    } else {
      const modeOpt = this.modeOptions.find(m =>
        m.mode === qso.mode && m.submode === qso.submode
      );
      modeSelection = modeOpt?.label || '';
    }

    this.qsoForm.patchValue({
      theirCallsign: qso.theirCallsign,
      qsoDate: new Date(qso.qsoDate),
      timeOn: qso.timeOn,
      band: qso.band,
      frequencyKhz: qso.frequencyKhz,
      modeSelection: modeSelection,
      customMode: qso.customMode || '',
      rstSent: qso.rstSent,
      rstRecv: qso.rstRecv,
      qth: qso.qth,
      gridSquare: qso.gridSquare,
      notes: qso.notes
    });
  }

  onModeSelectionChange(label: string): void {
    const modeOpt = this.modeOptions.find(m => m.label === label);
    this.selectedModeOption = modeOpt;
    this.showCustomModeField = modeOpt?.isCustom || false;

    if (!this.showCustomModeField) {
      this.qsoForm.patchValue({ customMode: '' });
    }
  }

  loadSuggestions(callsign: string): void {
    this.qsoService.getCallsignSuggestions(callsign).subscribe({
      next: (suggestion) => {
        if (suggestion.lastKnownQth && !this.qsoForm.get('qth')?.value) {
          this.qsoForm.patchValue({ qth: suggestion.lastKnownQth });
        }
        if (suggestion.lastNotesSnippet && !this.qsoForm.get('notes')?.value) {
          this.qsoForm.patchValue({ notes: suggestion.lastNotesSnippet });
        }
        if (suggestion.mostCommonBand && !this.qsoForm.get('band')?.value) {
          this.qsoForm.patchValue({ band: suggestion.mostCommonBand });
        }
      },
      error: () => {
        // Silently ignore - suggestions are optional
      }
    });
  }

  generateAiDescription(language: 'EN' | 'PL'): void {
    this.aiGenerating = true;
    this.aiLanguage = language;
    const formValue = this.qsoForm.value;

    const request = {
      theirCallsign: formValue.theirCallsign,
      qsoDate: this.formatDate(formValue.qsoDate),
      timeOn: formValue.timeOn,
      band: formValue.band,
      mode: this.selectedModeOption?.mode || formValue.modeSelection,
      rstSent: formValue.rstSent,
      rstRecv: formValue.rstRecv,
      qth: formValue.qth,
      notes: formValue.notes,
      language
    };

    this.aiService.generateQsoDescription(request).subscribe({
      next: (response) => {
        this.generatedText = response.text;
        this.aiGenerating = false;
      },
      error: () => {
        this.snackBar.open('AI generation failed', 'Close', { duration: 3000 });
        this.aiGenerating = false;
      }
    });
  }

  copyToNotes(): void {
    const currentNotes = this.qsoForm.get('notes')?.value || '';
    const newNotes = currentNotes ? `${currentNotes}\n\n${this.generatedText}` : this.generatedText;
    this.qsoForm.patchValue({ notes: newNotes });
    this.generatedText = '';
    this.snackBar.open('Copied to notes', 'Close', { duration: 2000 });
  }

  onSubmit(confirmDuplicate = false): void {
    if (this.qsoForm.invalid) {
      return;
    }

    this.loading = true;
    const request = this.buildRequest(confirmDuplicate);

    const save$ = this.isEditMode && this.qsoId
      ? this.qsoService.updateQso(this.qsoId, request)
      : this.qsoService.createQso(request);

    save$.subscribe({
      next: () => {
        this.snackBar.open('QSO saved successfully', 'Close', { duration: 2000 });
        this.router.navigate(['/qso']);
      },
      error: (error: HttpErrorResponse) => {
        if (error.status === 409 && !confirmDuplicate) {
          // Handle duplicate warning
          this.handleDuplicateWarning();
        } else {
          this.snackBar.open('Failed to save QSO', 'Close', { duration: 3000 });
        }
        this.loading = false;
      }
    });
  }

  handleDuplicateWarning(): void {
    const result = confirm('Potential duplicate QSO detected. Do you want to save anyway?');
    if (result) {
      this.savingWithConfirmDuplicate = true;
      this.onSubmit(true);
    }
  }

  buildRequest(confirmDuplicate: boolean): QsoRequest {
    const formValue = this.qsoForm.value;
    const modeOpt = this.selectedModeOption;

    let mode = modeOpt?.mode || 'SSB';
    let submode = modeOpt?.submode || null;
    let customMode = null;

    if (modeOpt?.isCustom && formValue.customMode) {
      customMode = formValue.customMode;
      mode = 'DATA';
      submode = null;
    }

    return {
      theirCallsign: formValue.theirCallsign.toUpperCase(),
      qsoDate: this.formatDate(formValue.qsoDate),
      timeOn: formValue.timeOn,
      band: formValue.band,
      frequencyKhz: formValue.frequencyKhz,
      mode,
      submode,
      customMode,
      rstSent: formValue.rstSent,
      rstRecv: formValue.rstRecv,
      qth: formValue.qth,
      gridSquare: formValue.gridSquare,
      notes: formValue.notes,
      confirmDuplicate
    };
  }

  private formatDate(date: Date): string {
    if (!date) return '';
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  cancel(): void {
    this.router.navigate(['/qso']);
  }
}
