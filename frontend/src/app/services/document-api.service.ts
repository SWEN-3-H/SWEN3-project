import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DocumentResponse } from '../models/document.model';

@Injectable({ providedIn: 'root' })
export class DocumentApiService {
  private http = inject(HttpClient);
  private baseUrl = '/api/documents';

  findAll() {
    return this.http.get<DocumentResponse[]>(this.baseUrl);
  }

  findById(id: number) {
    return this.http.get<DocumentResponse>(`${this.baseUrl}/${id}`);
  }

  upload(file: File) {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<DocumentResponse>(this.baseUrl, formData);
  }

  updateFilename(id: number, filename: string) {
    return this.http.put<DocumentResponse>(`${this.baseUrl}/${id}`, { filename });
  }

  delete(id: number) {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  addLabel(documentId: number, labelId: number) {
    return this.http.post<DocumentResponse>(`${this.baseUrl}/${documentId}/labels/${labelId}`, null);
  }

  removeLabel(documentId: number, labelId: number) {
    return this.http.delete<DocumentResponse>(`${this.baseUrl}/${documentId}/labels/${labelId}`);
  }
}
