export interface ProductAlertData {
  isAlertSet: boolean;
  targetPrice: number | null;
}

export interface ProductAlertRequest {
  targetPrice: number | null;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

function normalizeTargetPrice(value: unknown) {
  if (typeof value !== 'number' || value <= 0) {
    return null;
  }
  return value;
}

export function normalizeProductAlertResponse(payload: unknown): ProductAlertData {
  if (!isRecord(payload)) {
    return { isAlertSet: false, targetPrice: null };
  }

  const isAlertSet = payload.isAlertSet === true;
  const targetPrice = normalizeTargetPrice(payload.targetPrice);

  return {
    isAlertSet: isAlertSet && targetPrice !== null,
    targetPrice: isAlertSet ? targetPrice : null,
  };
}

export function createProductAlertRequest(targetPrice: number | null): ProductAlertRequest {
  return { targetPrice };
}
