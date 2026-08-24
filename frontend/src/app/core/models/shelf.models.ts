export type ShelfStatus = 'WANTS_TO_READ' | 'READING' | 'READ';

export interface ShelfResponse {
  id: number;
  bookIsbn: string;
  title: string;
  authorName: string;
  authorId: string | null;
  status: ShelfStatus;
  createdAt: string;
  updatedAt: string;
}
