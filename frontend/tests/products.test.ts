import test from 'node:test';
import assert from 'node:assert/strict';
import axios from 'axios';

import { CATEGORY_GROUPS } from '../src/constants/categories.ts';
import {
  DEFAULT_PRICE_HISTORY_DAYS,
  buildProductSearchParams,
  fetchPriceDroppedProducts,
  fetchPriceHistory,
  fetchProductDetail,
  fetchSimilarProducts,
} from '../src/api/products.ts';

test('builds product search params for a selected category group', () => {
  const skincare = CATEGORY_GROUPS.find(group => group.name === '스킨케어');

  assert.deepEqual(
    buildProductSearchParams({
      page: 2,
      keyword: '토너',
      category: '전체',
      selectedGroup: skincare,
    }),
    {
      page: 2,
      size: 20,
      keyword: '토너',
      category: undefined,
      categories: '스킨/토너,에센스/세럼/앰플,크림,로션,미스트/오일,스킨케어세트,스킨케어 디바이스',
      sort: 'updatedAt,desc',
    },
  );
});

test('builds product search params for a specific category', () => {
  assert.deepEqual(
    buildProductSearchParams({
      page: 0,
      keyword: '',
      category: '크림',
      selectedGroup: CATEGORY_GROUPS[0],
    }),
    {
      page: 0,
      size: 20,
      keyword: undefined,
      category: '크림',
      categories: undefined,
      sort: 'updatedAt,desc',
    },
  );
});

test('fetches product detail through the products API module', async () => {
  const originalGet = axios.get;
  const calls: Array<{ url: string; config?: unknown }> = [];

  axios.get = async (url: string, config?: unknown) => {
    calls.push({ url, config });
    return {
      data: {
        data: {
          id: 10,
          name: '테스트 상품',
          brand: '테스트 브랜드',
          currentPrice: 9000,
          originalPrice: 10000,
          discountRate: 10,
          imageUrl: 'https://example.com/image.jpg',
          productUrl: 'https://example.com/product',
          lowestPrice: 8000,
          highestPrice: 12000,
        },
      },
    };
  };

  try {
    const result = await fetchProductDetail('10');

    assert.equal(result.id, 10);
    assert.deepEqual(calls, [{ url: '/api/products/10', config: undefined }]);
  } finally {
    axios.get = originalGet;
  }
});

test('fetches price history with a days query parameter', async () => {
  const originalGet = axios.get;
  const calls: Array<{ url: string; config?: unknown }> = [];

  axios.get = async (url: string, config?: unknown) => {
    calls.push({ url, config });
    return {
      data: {
        data: [
          {
            currentPrice: 9000,
            recordedAt: '2026-05-26T09:00:00',
          },
        ],
      },
    };
  };

  try {
    const result = await fetchPriceHistory('10', 90);

    assert.equal(result[0].currentPrice, 9000);
    assert.deepEqual(calls, [{ url: '/api/products/10/prices', config: { params: { days: 90 } } }]);
  } finally {
    axios.get = originalGet;
  }
});

test('uses a 30 day default window for product detail price history', () => {
  assert.equal(DEFAULT_PRICE_HISTORY_DAYS, 30);
});

test('fetches price dropped products with category group params', async () => {
  const originalGet = axios.get;
  const calls: Array<{ url: string; config?: unknown }> = [];

  axios.get = async (url: string, config?: unknown) => {
    calls.push({ url, config });
    return {
      data: {
        data: [
          {
            id: 12,
            name: '가격하락 상품',
            brand: '테스트 브랜드',
            currentPrice: 8000,
            originalPrice: 10000,
            discountRate: 20,
            imageUrl: 'https://example.com/drop.jpg',
            isSale: true,
          },
        ],
      },
    };
  };

  try {
    const result = await fetchPriceDroppedProducts('스킨/토너,크림', 9);

    assert.equal(result[0].id, 12);
    assert.deepEqual(calls, [{ url: '/api/products/price-drops', config: { params: { categories: '스킨/토너,크림', size: 9 } } }]);
  } finally {
    axios.get = originalGet;
  }
});

test('fetches similar products from the products API module', async () => {
  const originalGet = axios.get;
  const calls: Array<{ url: string; config?: unknown }> = [];

  axios.get = async (url: string, config?: unknown) => {
    calls.push({ url, config });
    return {
      data: {
        data: [
          {
            id: 11,
            name: '추천 상품',
            brand: '테스트 브랜드',
            currentPrice: 8500,
            originalPrice: 10000,
            discountRate: 15,
            imageUrl: 'https://example.com/similar.jpg',
            isSale: true,
          },
        ],
      },
    };
  };

  try {
    const result = await fetchSimilarProducts('10');

    assert.equal(result[0].id, 11);
    assert.deepEqual(calls, [{ url: '/api/products/10/similar', config: undefined }]);
  } finally {
    axios.get = originalGet;
  }
});
