import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { useSearchParams } from 'react-router-dom';

import {
  ALL_CATEGORY_LABEL,
  getGroupNameForCategory,
  resolveSelectedCategoryGroup,
} from '../constants/categories';

const PRODUCTS_SECTION_ID = 'all-products-section';

function scrollToProducts() {
  document.getElementById(PRODUCTS_SECTION_ID)?.scrollIntoView({ behavior: 'smooth' });
}

export function useProductFilters() {
  const [searchParams, setSearchParams] = useSearchParams();

  const currentPage = parseInt(searchParams.get('page') || '0', 10);
  const keywordParam = searchParams.get('keyword') || '';
  const categoryParam = searchParams.get('category') || ALL_CATEGORY_LABEL;
  const groupParam = searchParams.get('group') || '';
  const selectedGroup = resolveSelectedCategoryGroup(groupParam, categoryParam);

  const [keywordInput, setKeywordInput] = useState(keywordParam);

  useEffect(() => {
    setKeywordInput(keywordParam);
  }, [keywordParam]);

  const updateSearchParams = useCallback((update: (nextParams: URLSearchParams) => void) => {
    const nextParams = new URLSearchParams(searchParams);
    update(nextParams);
    setSearchParams(nextParams);
    scrollToProducts();
  }, [searchParams, setSearchParams]);

  const submitSearch = useCallback((event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    updateSearchParams(nextParams => {
      const trimmedKeyword = keywordInput.trim();

      if (trimmedKeyword) {
        nextParams.set('keyword', trimmedKeyword);
      } else {
        nextParams.delete('keyword');
      }

      nextParams.set('page', '0');
    });
  }, [keywordInput, updateSearchParams]);

  const changePage = useCallback((pageIndex: number) => {
    updateSearchParams(nextParams => {
      nextParams.set('page', pageIndex.toString());
    });
  }, [updateSearchParams]);

  const changeGroup = useCallback((groupName: string) => {
    updateSearchParams(nextParams => {
      if (groupName) {
        nextParams.set('group', groupName);
      } else {
        nextParams.delete('group');
      }

      nextParams.delete('category');
      nextParams.set('page', '0');
    });
  }, [updateSearchParams]);

  const changeCategory = useCallback((categoryName: string) => {
    updateSearchParams(nextParams => {
      if (categoryName !== ALL_CATEGORY_LABEL) {
        nextParams.set('category', categoryName);

        const groupName = getGroupNameForCategory(categoryName);
        if (groupName) {
          nextParams.set('group', groupName);
        }
      } else {
        nextParams.delete('category');
        nextParams.delete('group');
      }

      nextParams.set('page', '0');
    });
  }, [updateSearchParams]);

  return {
    currentPage,
    keywordParam,
    categoryParam,
    selectedGroup,
    keywordInput,
    setKeywordInput,
    submitSearch,
    changePage,
    changeGroup,
    changeCategory,
  };
}
