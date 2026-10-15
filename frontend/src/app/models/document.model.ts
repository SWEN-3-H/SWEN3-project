import { LabelResponse } from './label.model';

export interface DocumentResponse {
  id: number;
  filename: string;
  createdAt: string;
  summary: string | null;
  labels: LabelResponse[];
}
