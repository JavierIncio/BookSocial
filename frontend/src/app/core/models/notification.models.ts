export interface NotificationResponse {
  id: string;
  type: string;
  payload: { followerId?: number };
  read: boolean;
  occurredAt: string;
}