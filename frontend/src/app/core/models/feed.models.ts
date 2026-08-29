export type FeedActivityType = 'FOLLOW' | 'REVIEW' | 'SHELF';

export interface FeedPayload {
  targetUserId?: number;
  bookIsbn?: string;
  title?: string;
  authorName?: string;
  rating?: number;
  comment?: string;
  shelfStatus?: string;
}

export interface FeedItemResponse {
  activityId: string;
  type: FeedActivityType;
  actorId: number;
  payload: FeedPayload;
  occurredAt: string;
}

export interface FeedPageResponse {
  items: FeedItemResponse[];
  nextCursor: string | null;
}