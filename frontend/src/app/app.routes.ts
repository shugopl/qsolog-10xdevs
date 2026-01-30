import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { LayoutComponent } from './shared/layout/layout.component';
import { QsoListComponent } from './features/qso/qso-list/qso-list.component';
import { QsoFormComponent } from './features/qso/qso-form/qso-form.component';
import { StatsComponent } from './features/stats/stats.component';
import { AiReportsComponent } from './features/ai/ai-reports/ai-reports.component';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'qso', component: QsoListComponent },
      { path: 'qso/new', component: QsoFormComponent },
      { path: 'qso/:id/edit', component: QsoFormComponent },
      { path: 'stats', component: StatsComponent },
      { path: 'ai/reports', component: AiReportsComponent }
    ]
  }
];
