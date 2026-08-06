import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { UserResponse } from '@core/models/user.models';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  constructor(private readonly http: HttpClient) {}

  me(): Observable<UserResponse> {
    return this.http.get<UserResponse>('/users/me');
  }
}
