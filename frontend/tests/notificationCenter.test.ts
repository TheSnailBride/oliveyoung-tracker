import test from 'node:test';
import assert from 'node:assert/strict';

import {
  normalizeNotificationCenterResponse,
  removeNotificationFromCenter,
} from '../src/api/notifications.ts';

test('normalizes notification center payload into notifications and active alerts arrays', () => {
  const payload = {
    notifications: [
      {
        id: 1,
        productId: 10,
        productName: '알림 상품',
        productImageUrl: 'https://example.com/notification.jpg',
        priceAtAlert: 9900,
        isRead: false,
        createdAt: '2026-05-25T09:30:00',
      },
    ],
    activeAlerts: [
      {
        id: 2,
        productId: 11,
        productName: '설정 상품',
        productImageUrl: 'https://example.com/alert.jpg',
        targetPrice: 12000,
        currentPrice: 13500,
        createdAt: '2026-05-24T08:00:00',
      },
    ],
  };

  const result = normalizeNotificationCenterResponse(payload);

  assert.equal(result.notifications.length, 1);
  assert.equal(result.notifications[0].priceAtAlert, 9900);
  assert.equal(result.activeAlerts.length, 1);
  assert.equal(result.activeAlerts[0].targetPrice, 12000);
});

test('normalizes missing notification center arrays to empty arrays', () => {
  const result = normalizeNotificationCenterResponse({});

  assert.deepEqual(result.notifications, []);
  assert.deepEqual(result.activeAlerts, []);
});

test('removes only the selected arrived notification from notification center data', () => {
  const result = removeNotificationFromCenter({
    notifications: [
      {
        id: 1,
        productId: 10,
        productName: '삭제할 알림',
        productImageUrl: '',
        priceAtAlert: 9900,
        isRead: true,
        createdAt: '2026-05-25T09:30:00',
      },
      {
        id: 2,
        productId: 11,
        productName: '남길 알림',
        productImageUrl: '',
        priceAtAlert: 11000,
        isRead: true,
        createdAt: '2026-05-25T10:30:00',
      },
    ],
    activeAlerts: [
      {
        id: 3,
        productId: 12,
        productName: '진행 중인 알림',
        productImageUrl: '',
        targetPrice: 12000,
        currentPrice: 13000,
        createdAt: '2026-05-24T08:00:00',
      },
    ],
  }, 1);

  assert.deepEqual(result.notifications.map(notification => notification.id), [2]);
  assert.deepEqual(result.activeAlerts.map(alert => alert.id), [3]);
});
