import {Component, OnInit, inject, signal, computed} from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { DocumentApiService } from '../../services/document-api.service';
import { DocumentResponse } from '../../models/document.model';
import { ConfirmDialog } from '../../components/confirm-dialog/confirm-dialog';
import { MatSelectModule } from '@angular/material/select';
import { LabelApiService } from '../../services/label-api.service';
import { LabelResponse } from '../../models/label.model';
import {LabelChip} from '../../components/label-chip/label-chip';

type LoadState = 'loading' | 'ready' | 'notFound' | 'error';

@Component({
  selector: 'app-document-detail',
  imports: [
    DatePipe, ReactiveFormsModule, RouterLink,
    MatButtonModule, MatCardModule, MatFormFieldModule,
    MatIconModule, MatInputModule, MatProgressSpinnerModule,
    MatSnackBarModule, MatSelectModule, LabelChip,
  ],
  templateUrl: './document-detail.html',
  styleUrl: './document-detail.css',
})
export class DocumentDetail implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private api = inject(DocumentApiService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  readonly document = signal<DocumentResponse | null>(null);
  readonly state = signal<LoadState>('loading');
  readonly editing = signal(false);
  readonly saving = signal(false);
  readonly renameError = signal<string | null>(null);

  private labelApi = inject(LabelApiService);

  readonly allLabels = signal<LabelResponse[]>([]);
  readonly labelControl = new FormControl<number | null>(null);
  readonly labelBusy = signal(false);

  readonly availableLabels = computed(() => {
    const assigned = new Set(this.document()?.labels.map(l => l.id));
    return this.allLabels().filter(l => !assigned.has(l.id));
  });

  readonly filenameControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(255)],
  });

  ngOnInit() {
    this.route.paramMap.subscribe(params =>
      this.load(Number(params.get('id')))
    );

    this.labelApi.findAll().subscribe({
      next: labels => this.allLabels.set(labels),
      error: () => this.snackBar.open(
        'Labels could not be loaded.',
        'OK',
        { duration: 5000 }
      ),
    });
  }

  private load(id: number) {
    this.state.set('loading');
    this.editing.set(false);
    this.api.findById(id).subscribe({
      next: doc => {
        this.document.set(doc);
        this.state.set('ready');
      },
      error: err => this.state.set(err.status === 404 ? 'notFound' : 'error'),
    });
  }

  startEdit() {
    this.filenameControl.setValue(this.document()!.filename);
    this.renameError.set(null);
    this.editing.set(true);
  }

  cancelEdit() {
    this.editing.set(false);
  }

  saveName() {
    const doc = this.document();
    if (!doc || this.filenameControl.invalid || this.saving()) {
      this.filenameControl.markAsTouched();
      return;
    }

    this.saving.set(true);
    this.api.updateFilename(doc.id, this.filenameControl.value.trim()).subscribe({
      next: updated => {
        this.document.set(updated);
        this.editing.set(false);
        this.saving.set(false);
        this.snackBar.open('Filename saved', 'OK', { duration: 3000 });
      },
      error: err => {
        this.saving.set(false);
        this.renameError.set(this.errorMessage(err.status));
      },
    });
  }

  deleteDocument() {
    const doc = this.document();
    if (!doc) return;

    this.dialog
      .open(ConfirmDialog, {
        data: {
          title: 'Delete document?',
          message: `"${doc.filename}" will be permanently deleted.`,
          confirmLabel: 'Delete',
        },
      })
      .afterClosed()
      .subscribe(confirmed => {
        if (!confirmed) return;
        this.api.delete(doc.id).subscribe({
          next: () => {
            this.snackBar.open('Document deleted', 'OK', { duration: 3000 });
            this.router.navigate(['/']);
          },
          error: err =>
            this.snackBar.open(this.errorMessage(err.status), 'OK', { duration: 5000 }),
        });
      });
  }

  addLabel() {
    const doc = this.document();
    const labelId = this.labelControl.value;
    if (!doc || labelId === null || this.labelBusy()) return;

    this.labelBusy.set(true);
    this.api.addLabel(doc.id, labelId).subscribe({
      next: updated => {
        this.document.set(updated);
        this.labelControl.reset(null);
        this.labelBusy.set(false);
      },
      error: err => {
        this.labelBusy.set(false);
        this.snackBar.open(this.errorMessage(err.status), 'OK', { duration: 5000 });
      },
    });
  }

  removeLabel(label: LabelResponse) {
    const doc = this.document();
    if (!doc || this.labelBusy()) return;

    this.labelBusy.set(true);
    this.api.removeLabel(doc.id, label.id).subscribe({
      next: updated => {
        this.document.set(updated);
        this.labelBusy.set(false);
      },
      error: err => {
        this.labelBusy.set(false);
        this.snackBar.open(this.errorMessage(err.status), 'OK', { duration: 5000 });
      },
    });
  }

  private errorMessage(status: number): string {
    switch (status) {
      case 0:   return 'Server not reachable.';
      case 400: return 'Invalid input.';
      case 404: return 'This document no longer exists.';
      default:  return 'Action failed.';
    }
  }
}
