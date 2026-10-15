import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { LabelResponse } from '../models/label.model';

@Injectable({ providedIn: 'root' })
export class LabelApiService {
  private http = inject(HttpClient);
  private baseUrl = '/api/labels';

  findAll() {
    return this.http.get<LabelResponse[]>(this.baseUrl);
  }

  create(name: string) {
    return this.http.post<LabelResponse>(this.baseUrl, { name });
  }

  delete(id: number) {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
