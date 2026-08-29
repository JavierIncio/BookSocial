import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ShelfService } from '@core/services/shelf.service';
import { ShelfResponse, ShelfStatus } from '@core/models/shelf.models';
import { Nav } from '@shared/components/nav/nav';

@Component({
  selector: 'app-my-shelf',
  imports: [RouterLink, Nav],
  templateUrl: './my-shelf.html',
  styleUrl: './my-shelf.scss',
})
export class MyShelf implements OnInit {
  private readonly shelfService = inject(ShelfService);

  readonly filters: { value: ShelfStatus | null; label: string }[] = [
    { value: null, label: $localize`:@@shelfFilterAll:All` },
    { value: 'WANTS_TO_READ', label: $localize`:@@shelfStatusWantToRead:Want to read` },
    { value: 'READING', label: $localize`:@@shelfStatusReading:Reading` },
    { value: 'READ', label: $localize`:@@shelfStatusRead:Read` },
  ];

  private readonly statusLabels: Record<ShelfStatus, string> = {
    WANTS_TO_READ: $localize`:@@shelfStatusWantToRead:Want to read`,
    READING: $localize`:@@shelfStatusReading:Reading`,
    READ: $localize`:@@shelfStatusRead:Read`,
  };

  shelves = signal<ShelfResponse[]>([]);
  loading = signal<boolean>(true);
  error = signal<string>('');
  filter = signal<ShelfStatus | null>(null);

  filtered = computed(() => {
    const current = this.filter();
    const all = this.shelves();
    return current ? all.filter((s) => s.status === current) : all;
  });

  ngOnInit(): void {
    this.shelfService.mine().subscribe({
      next: (shelves) => {
        this.shelves.set(shelves);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set($localize`:@@shelfErrorLoad:Failed to load your shelf.`);
      },
    });
  }

  setFilter(value: ShelfStatus | null): void {
    this.filter.set(value);
  }

  label(status: ShelfStatus): string {
    return this.statusLabels[status];
  }
}
