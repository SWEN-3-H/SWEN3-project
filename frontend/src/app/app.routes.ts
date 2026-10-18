import { Routes } from '@angular/router';
import { Home } from './pages/home/home';
import { DocumentDetail } from './pages/document-detail/document-detail';
import { Upload } from './pages/upload/upload';
import { Labels } from './pages/labels/labels';

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'documents/:id', component: DocumentDetail },
  { path: 'upload', component: Upload },
  { path: 'labels', component: Labels },
  { path: '**', redirectTo: '' }
];
