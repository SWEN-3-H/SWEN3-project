import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { LabelApiService } from '../../services/label-api.service';
import { LabelResponse } from '../../models/label.model';
import { LABEL_COLORS, DEFAULT_LABEL_COLOR } from '../../models/label-colors';
import { ConfirmDialog } from '../../components/confirm-dialog/confirm-dialog';
import { LabelChip } from '../../components/label-chip/label-chip';

@Component({
  selector: 'app-labels',
  imports: [
    ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule,
    MatInputModule, MatListModule, MatProgressSpinnerModule, MatSnackBarModule,
    LabelChip,
  ],
  templateUrl: './labels.html',
  styleUrl: './labels.css',
})
export class Labels implements OnInit {
  private api = inject(LabelApiService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  readonly labels = signal<LabelResponse[]>([]);
  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly saving = signal(false);
  readonly formError = signal<string | null>(null);
  readonly editing = signal<LabelResponse | null>(null);

  readonly colors = LABEL_COLORS;
  readonly selectedColor = signal(DEFAULT_LABEL_COLOR);

  readonly nameControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(20)],
  });

  ngOnInit() {
    this.api.findAll().subscribe({
      next: labels => {
        this.labels.set(labels);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set('Labels could not be loaded.');
        this.loading.set(false);
      },
    });
  }

  startEdit(label: LabelResponse) {
    this.editing.set(label);
    this.nameControl.setValue(label.name);
    this.selectedColor.set(label.color ?? DEFAULT_LABEL_COLOR);
    this.formError.set(null);
  }

  cancelEdit() {
    this.editing.set(null);
    this.nameControl.reset('');
    this.formError.set(null);
  }

  submit() {
    if (this.nameControl.invalid || this.saving()) {
      this.nameControl.markAsTouched();
      return;
    }

    const name = this.nameControl.value.trim();
    const color = this.selectedColor();
    const editing = this.editing();

    this.saving.set(true);
    this.formError.set(null);

    if (editing) {
      this.api.update(editing.id, name, color).subscribe({
        next: updated => {
          this.labels.update(list =>
            list.map(l => (l.id === updated.id ? updated : l))
              .sort((a, b) => a.name.localeCompare(b.name))
          );
          this.cancelEdit();
          this.saving.set(false);
          this.snackBar.open('Label saved', 'OK', { duration: 3000 });
        },
        error: err => this.handleSaveError(err.status, editing),
      });
    } else {
      this.api.create(this.nameControl.value.trim(), this.selectedColor()).subscribe({
        next: label => {
          this.labels.update(list =>
            [...list, label].sort((a, b) => a.name.localeCompare(b.name))
          );
          this.nameControl.reset('');
          this.saving.set(false);
          this.snackBar.open('Label created', 'OK', { duration: 3000 });
        },
        error: err => this.handleSaveError(err.status, null),
      });
    }
  }

  private handleSaveError(status: number, editing: LabelResponse | null) {
    this.saving.set(false);

    if (status === 404 && editing) {
      this.labels.update(list => list.filter(l => l.id !== editing.id));
      this.cancelEdit();
      this.formError.set('This label no longer exists.');
      return;
    }

    this.formError.set(
      status === 409 ? 'A label with this name already exists.'
        : status === 0 ? 'Server not reachable.'
          : status === 400 ? 'Invalid input.'
            : 'Label could not be saved.'
    );
  }

  delete(label: LabelResponse) {
    this.dialog
      .open(ConfirmDialog, {
        data: {
          title: 'Delete label?',
          message: `"${label.name}" will be removed from all documents.`,
          confirmLabel: 'Delete',
        },
      })
      .afterClosed()
      .subscribe(confirmed => {
        if (!confirmed) return;
        this.api.delete(label.id).subscribe({
          next: () => {
            if (this.editing()?.id === label.id) this.cancelEdit();
            this.labels.update(list => list.filter(l => l.id !== label.id));
            this.snackBar.open('Label deleted', 'OK', { duration: 3000 });
          },
          error: err =>
            this.snackBar.open(
              err.status === 404 ? 'This label no longer exists.' : 'Label could not be deleted.',
              'OK', { duration: 5000 }
            ),
        });
      });
  }
}
