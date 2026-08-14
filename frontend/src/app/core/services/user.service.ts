import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { UserResponse } from '@core/models/user.models';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private readonly http = inject(HttpClient);

  me(): Observable<UserResponse> {
    return this.http.get<UserResponse>('/users/me');
  }
}
