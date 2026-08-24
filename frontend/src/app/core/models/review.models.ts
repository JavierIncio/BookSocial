export interface ReviewResponse {
  id: number;
  bookIsbn: string;
  userId: number;
  rating: number;
  comment: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ReviewSummaryResponse {
  bookIsbn: string;
  ratingCount: number;
  averageRating: number;
}
