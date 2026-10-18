import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DocumentApiService } from '../../services/document-api.service';
import { DocumentResponse } from '../../models/document.model';
import {LabelChip} from '../../components/label-chip/label-chip';

@Component({
  selector: 'app-home',
  imports: [
    RouterLink, DatePipe,
    MatButtonModule, MatIconModule, MatTableModule,
    MatProgressSpinnerModule, LabelChip,
  ],
  templateUrl: './home.html',
  styleUrl: './home.css'
})
export class Home implements OnInit {
  private readonly api = inject(DocumentApiService);

  readonly documents = signal<DocumentResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  displayedColumns = ['position', 'filename', 'createdAt', 'labels'];

  ngOnInit() {
    this.api.findAll().subscribe({
      next: docs => {
        this.documents.set(docs);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load documents');
        this.loading.set(false);
      }
    });
  }
}
