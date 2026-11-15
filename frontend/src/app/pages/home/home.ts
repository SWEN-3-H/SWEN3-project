import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { DocumentApiService } from '../../services/document-api.service';
import { DocumentResponse } from '../../models/document.model';
import {LabelChip} from '../../components/label-chip/label-chip';

@Component({
  selector: 'app-home',
  imports: [
    RouterLink, DatePipe,
    MatButtonModule, MatIconModule, MatTableModule,
    MatProgressSpinnerModule, MatFormFieldModule, MatInputModule, LabelChip,
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

  /*ngOnInit() {
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
  } */

  ngOnInit() {
    this.search('');
  }

  search(term: string) {
    const query = term.trim();
    this.loading.set(true);
    this.error.set(null);

    const request = query ? this.api.search(query) : this.api.findAll();
    request.subscribe({
      next: docs => {
        this.documents.set(docs);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(query ? 'Search failed' : 'Could not load documents');
        this.loading.set(false);
      }
    });
  }
}
