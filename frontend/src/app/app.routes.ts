import { Routes } from '@angular/router';
import { Home } from './pages/home/home';
import { Documents } from './pages/documents/documents';
import { Upload } from './pages/upload/upload';


export const routes: Routes = [
  { path: '', component: Home },
  { path: 'upload', component: Upload },
  { path: 'documents', component: Documents },
];
