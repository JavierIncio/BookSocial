import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
  type AbstractControl,
  type ValidationErrors,
  type ValidatorFn,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { environment } from '@env/environments';
import { AuthService } from '@core/services/auth.service';

const pastDate: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = control.value;
  if (!value) return null;
  const date = new Date(value);
  const todayStart = new Date().setHours(0, 0, 0, 0);
  return date.getTime() >= todayStart ? { notPast: true } : null;
};

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly form = this.fb.nonNullable.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    birthDate: ['', [Validators.required, pastDate]],
  });

  errorMessage = signal<string>('');
  loading = signal<boolean>(false);

  submit(): void {
    if (this.form.invalid) return;

    this.loading.set(true);
    this.errorMessage.set('');
    this.auth.register(this.form.getRawValue()).subscribe({
      next: () => this.router.navigate(['/home']),
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.errorMessage.set(
          error.status === 409
            ? $localize`:@@registerErrorDuplicate:There is already an account with that email.`
            : error.status === 400
              ? $localize`:@@registerErrorInvalidForm:Please check the form data.`
              : $localize`:@@registerErrorUnexpected:Unexpected error. Please try again.`,
        );
      },
    });
  }

  loginWithGoogle(): void {
    window.location.href = environment.googleAuthUrl;
  }
}
