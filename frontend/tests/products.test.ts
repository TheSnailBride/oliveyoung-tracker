import test from 'node:test';
import assert from 'node:assert/strict';

import { CATEGORY_GROUPS } from '../src/constants/categories.ts';
import { buildProductSearchParams } from '../src/api/products.ts';

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
