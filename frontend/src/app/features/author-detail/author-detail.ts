import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthorService } from '@core/services/author.service';
import { AuthorResponse, WorkEntry } from '@core/models/author.models';
import { Nav } from '@shared/components/nav/nav';

@Component({
  selector: 'app-author-detail',
  imports: [RouterLink, Nav],
  templateUrl: './author-detail.html',
  styleUrl: './author-detail.scss',
})
export class AuthorDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly authorService = inject(AuthorService);

  author = signal<AuthorResponse | null>(null);
  works = signal<WorkEntry[]>([]);
  loading = signal<boolean>(true);
  error = signal<string>('');

  ngOnInit(): void {
    const raw = this.route.snapshot.paramMap.get('authorId');
    const id = Number(raw);
    if (!raw || !Number.isInteger(id) || id <= 0) {
      this.loading.set(false);
      this.error.set('Invalid author.');
      return;
    }

    this.authorService.byInternalId(id).subscribe({
      next: (author) => {
        this.loading.set(false);
        if (!author) {
          this.error.set('Author not found.');
          return;
        }
        this.author.set(author);
        if (author.openLibraryId) {
          this.loadWorks(author.openLibraryId);
        }
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Could not load the author.');
      },
    });
  }

  private loadWorks(openLibraryId: string): void {
    this.authorService.works(openLibraryId).subscribe({
      next: (response) => this.works.set(response.entries ?? []),
      error: () => this.works.set([]),
    });
  }

  initials(name: string): string {
    return name
      .split(/\s+/)
      .filter((part) => part.length > 0)
      .slice(0, 2)
      .map((part) => part[0].toUpperCase())
      .join('');
  }

  workCover(entry: WorkEntry): string | null {
    const coverId = entry.covers?.find((c) => c > 0);
    return coverId ? `https://covers.openlibrary.org/b/id/${coverId}-M.jpg` : null;
  }
}
