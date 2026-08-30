import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '@core/services/auth.service';

@Component({
  selector: 'app-reset-password',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.scss',
})
export class ResetPassword implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);

  token = '';

  readonly form = this.fb.nonNullable.group(
    {
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirm: ['', Validators.required],
    },
    {
      validators: (group) =>
        group.get('password')?.value === group.get('confirm')?.value
          ? null
          : { mismatch: true },
    },
  );

  loading = signal<boolean>(false);
  done = signal<boolean>(false);
  invalidToken = signal<boolean>(false);
  errorMessage = signal<string>('');

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (!this.token) {
      this.invalidToken.set(true);
    }
  }

  submit(): void {
    if (this.form.invalid || !this.token) return;

    this.loading.set(true);
    this.errorMessage.set('');
    this.auth.resetPassword(this.token, this.form.getRawValue().password).subscribe({
      next: () => {
        this.loading.set(false);
        this.done.set(true);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        const code = (error.error as { error?: string } | undefined)?.error;
        if (code === 'INVALID_TOKEN') {
          this.invalidToken.set(true);
        } else if (code === 'EXPIRED_TOKEN') {
          this.errorMessage.set($localize`:@@resetErrorExpired:This link has expired. Please request a new one.`);
        } else if (code === 'ALREADY_USED') {
          this.errorMessage.set($localize`:@@resetErrorUsed:This link has already been used. Please request a new one.`);
        } else {
          this.errorMessage.set(
            $localize`:@@resetErrorUnexpected:Unexpected error. Please try again.`,
          );
        }
      },
    });
  }
}
