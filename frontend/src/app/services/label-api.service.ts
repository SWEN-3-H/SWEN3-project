import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { LabelResponse } from '../models/label.model';

@Injectable({ providedIn: 'root' })
export class LabelApiService {
  private http = inject(HttpClient);
  private baseUrl = '/api/labels';

  create(name: string, color: string | null) {
    return this.http.post<LabelResponse>(this.baseUrl, { name, color });
  }

  findAll() {
    return this.http.get<LabelResponse[]>(this.baseUrl);
  }

  update(id: number, name: string, color: string | null) {
    return this.http.put<LabelResponse>(`${this.baseUrl}/${id}`, { name, color });
  }

  delete(id: number) {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
