import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { DocumentApiService } from '../../services/document-api.service';
import { DecimalPipe } from '@angular/common';

type UploadState = 'pending' | 'uploading' | 'done' | 'error';

interface UploadItem {
  file: File;
  state: UploadState;
  message?: string;
}

const MAX_SIZE = 50 * 1024 * 1024;

@Component({
  selector: 'app-upload',
  imports: [MatButtonModule, MatIconModule, MatListModule, MatSnackBarModule, DecimalPipe],
  templateUrl: './upload.html',
  styleUrl: './upload.css',
})
export class Upload {
  private api = inject(DocumentApiService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  readonly items = signal<UploadItem[]>([]);
  readonly rejected = signal<string[]>([]);
  readonly isHovering = signal(false);

  readonly uploading = computed(() => this.items().some(i => i.state === 'uploading'));
  readonly canUpload = computed(
    () => !this.uploading() && this.items().some(i => i.state === 'pending' || i.state === 'error')
  );

  dragOver(event: DragEvent) {
    event.preventDefault();
    this.isHovering.set(true);
  }

  dragLeave(event: DragEvent) {
    event.preventDefault();
    this.isHovering.set(false);
  }

  drop(event: DragEvent) {
    event.preventDefault();
    this.isHovering.set(false);
    if (event.dataTransfer?.files.length) {
      this.addFiles(event.dataTransfer.files);
    }
  }

  fileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files?.length) {
      this.addFiles(input.files);
    }
    input.value = '';
  }

  private addFiles(fileList: FileList) {
    const errors: string[] = [];
    const accepted: UploadItem[] = [];

    for (const file of Array.from(fileList)) {
      const isPdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');
      const duplicate = this.items().some(
        i => i.file.name === file.name && i.file.size === file.size
      );

      if (!isPdf) {
        errors.push(`${file.name}: only PDF files are allowed.`);
      } else if (file.size > MAX_SIZE) {
        errors.push(`${file.name}: file is larger than 50 MB.`);
      } else if (file.size === 0) {
        errors.push(`${file.name}: file is empty.`);
      } else if (duplicate) {
        errors.push(`${file.name}: already in the list.`);
      } else {
        accepted.push({ file, state: 'pending' });
      }
    }

    this.rejected.set(errors);
    this.items.update(list => [...list, ...accepted]);
  }

  removeFile(item: UploadItem) {
    if (this.uploading() || item.state === 'done') {
      return;
    }

    this.items.update(list =>
      list.filter(current => current.file !== item.file)
    );
  }

  upload() {
    const toSend = this.items().filter(i => i.state === 'pending' || i.state === 'error');
    toSend.forEach(item => this.sendOne(item));
  }

  private sendOne(item: UploadItem) {
    this.setState(item, 'uploading');

    this.api.upload(item.file).subscribe({
      next: () => {
        this.setState(item, 'done');
        this.checkFinished();
      },
      error: err => {
        this.setState(item, 'error', this.errorMessage(err.status));
      },
    });
  }

  private setState(
    item: UploadItem,
    state: UploadState,
    message?: string
  ) {
    this.items.update(list =>
      list.map(current =>
        current.file === item.file
          ? { ...current, state, message }
          : current
      )
    );
  }

  private errorMessage(status: number): string {
    switch (status) {
      case 0:   return 'Server not reachable.';
      case 400: return 'Invalid request.';
      case 413: return 'File is too large.';
      default:  return 'Upload failed.';
    }
  }

  private checkFinished() {
    if (this.items().every(i => i.state === 'done')) {
      this.snackBar.open('Documents uploaded', 'OK', { duration: 3000 });
      this.router.navigate(['/']);
    }
  }
}
