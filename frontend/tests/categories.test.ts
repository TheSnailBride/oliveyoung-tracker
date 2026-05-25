import test from 'node:test';
import assert from 'node:assert/strict';

import {
  ALL_CATEGORY_LABEL,
  CATEGORY_GROUPS,
  getCategoriesParamForGroup,
  getGroupNameForCategory,
  resolveSelectedCategoryGroup,
} from '../src/constants/categories.ts';

test('resolves selected category group from group or category params', () => {
  assert.equal(resolveSelectedCategoryGroup('스킨케어', ALL_CATEGORY_LABEL)?.name, '스킨케어');
  assert.equal(resolveSelectedCategoryGroup('', '크림')?.name, '스킨케어');
  assert.equal(resolveSelectedCategoryGroup('', ALL_CATEGORY_LABEL), undefined);
});

test('builds comma separated category params for a selected group', () => {
  const skincare = CATEGORY_GROUPS.find(group => group.name === '스킨케어');

  assert.equal(
    getCategoriesParamForGroup(skincare),
    '스킨/토너,에센스/세럼/앰플,크림,로션,미스트/오일,스킨케어세트,스킨케어 디바이스',
  );
});

test('finds parent group name for a specific category', () => {
  assert.equal(getGroupNameForCategory('크림'), '스킨케어');
  assert.equal(getGroupNameForCategory('존재하지 않는 카테고리'), undefined);
});
