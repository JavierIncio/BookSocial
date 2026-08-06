export interface ApiError {
  error: string;
  message?: string;
  fields?: Record<string, string>;
}

export type ApiErrorCode =
  'email_already_exists' | 'bad_credentials' | 'invalid_refresh_token' | 'validation_failed';
