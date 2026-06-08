import test from 'node:test';
import assert from 'node:assert/strict';

import { buildSearchResultsPath } from '../src/utils/searchRoutes.ts';

test('builds a search result path from a keyword', () => {
  assert.equal(buildSearchResultsPath(' 양배추 '), '/search?keyword=%EC%96%91%EB%B0%B0%EC%B6%94&page=0');
});

test('returns null for an empty search keyword', () => {
  assert.equal(buildSearchResultsPath('   '), null);
});
