export interface NotificationItem {
  id: number;
  productId: number;
  productName: string;
  productImageUrl: string;
  priceAtAlert: number;
  isRead: boolean;
  createdAt: string;
}

export interface ActiveAlertItem {
  id: number;
  productId: number;
  productName: string;
  productImageUrl: string;
  targetPrice: number | null;
  currentPrice: number | null;
  createdAt: string;
}

export interface NotificationCenterData {
  notifications: NotificationItem[];
  activeAlerts: ActiveAlertItem[];
}

const EMPTY_NOTIFICATION_CENTER: NotificationCenterData = {
  notifications: [],
  activeAlerts: [],
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

export function normalizeNotificationCenterResponse(payload: unknown): NotificationCenterData {
  if (!isRecord(payload)) {
    return EMPTY_NOTIFICATION_CENTER;
  }

  return {
    notifications: Array.isArray(payload.notifications)
      ? payload.notifications as NotificationItem[]
      : [],
    activeAlerts: Array.isArray(payload.activeAlerts)
      ? payload.activeAlerts as ActiveAlertItem[]
      : [],
  };
}

export function removeNotificationFromCenter(
  center: NotificationCenterData,
  notificationId: number
): NotificationCenterData {
  return {
    notifications: center.notifications.filter(notification => notification.id !== notificationId),
    activeAlerts: center.activeAlerts,
  };
}
