import { inject, Injectable, signal } from '@angular/core';
import { Client } from '@stomp/stompjs';
import { AuthService } from '@core/services/auth.service';
import { NotificationResponse } from '@core/models/notification.models';
import { environment } from '@env/environments';

@Injectable({ providedIn: 'root' })
export class NotificationRealtimeService {
  private readonly auth = inject(AuthService);

  private client: Client | null = null;

  readonly connected = signal<boolean>(false);

  private wsUrl(token: string): string {
    const base =
      environment.notificationWsUrl ??
      `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}/ws`;
    return `${base}?token=${encodeURIComponent(token)}`;
  }

  connect(onNotification: (notification: NotificationResponse) => void): void {
    this.disconnect();

    const token = this.auth.accessToken();
    const userId = this.auth.userId();
    if (!token || userId === null) return;

    this.client = new Client({
      brokerURL: this.wsUrl(token),
      reconnectDelay: 5000,
    });

    this.client.onConnect = () => {
      this.connected.set(true);
      this.client?.subscribe(`/topic/notifications/${userId}`, (frame) => {
        try {
          onNotification(JSON.parse(frame.body) as NotificationResponse);
        } catch {
          // Ignore malformed STOMP frames.
        }
      });
    };

    this.client.onWebSocketClose = () => this.connected.set(false);
    this.client.onStompError = () => this.connected.set(false);

    this.client.activate();
  }

  disconnect(): void {
    if (this.client) {
      try {
        this.client.deactivate();
      } catch {
        // Ignored on teardown.
      }
    }
    this.client = null;
    this.connected.set(false);
  }
}