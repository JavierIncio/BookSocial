import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { ProfileResponse, UserResponse } from '@core/models/user.models';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private readonly http = inject(HttpClient);

  me(): Observable<UserResponse> {
    return this.http.get<UserResponse>('/api/users/me');
  }

  profile(userId: number): Observable<ProfileResponse> {
    return this.http.get<ProfileResponse>(`/profiles/${userId}`);
  }

  myProfile(): Observable<ProfileResponse> {
    return this.http.get<ProfileResponse>('/profiles/me');
  }
}
