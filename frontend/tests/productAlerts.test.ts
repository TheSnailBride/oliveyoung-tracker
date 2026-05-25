import test from 'node:test';
import assert from 'node:assert/strict';

import {
  createProductAlertRequest,
  normalizeProductAlertResponse,
} from '../src/api/productAlerts.ts';

test('normalizes an enabled product alert response', () => {
  const result = normalizeProductAlertResponse({
    isAlertSet: true,
    targetPrice: 12000,
  });

  assert.equal(result.isAlertSet, true);
  assert.equal(result.targetPrice, 12000);
});

test('normalizes disabled or legacy product alert response to null target price', () => {
  const result = normalizeProductAlertResponse({
    isAlertSet: false,
    targetPrice: -1,
  });

  assert.equal(result.isAlertSet, false);
  assert.equal(result.targetPrice, null);
});

test('creates a null target price request when clearing an alert', () => {
  assert.deepEqual(createProductAlertRequest(null), { targetPrice: null });
});
