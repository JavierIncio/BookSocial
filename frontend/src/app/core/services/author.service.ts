import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({providedIn: 'root'})
export class AuthorService {
  private readonly http = inject(HttpClient);

  search(q: string): Observable<A> {

}
