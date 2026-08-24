export interface BookResponse {
  isbn: string;
  title: string;
  authorName: string;
  authorId: string | null;
  description: string | null;
  coverUrl: string | null;
  publishedYear: number | null;
  category: string | null;
  createdAt: string; // Instant llega como string en formato ISO 8601 ('YYYY-MM-DDTHH:mm:ss.sssZ')
}
