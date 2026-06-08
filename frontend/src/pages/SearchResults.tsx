import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Search } from 'lucide-react';

import { fetchProducts, type PageData } from '../api/products';
import Pagination from '../components/Pagination';
import ProductCard from '../components/ProductCard';
import { ALL_CATEGORY_LABEL } from '../constants/categories';
import { buildSearchResultsPath } from '../utils/searchRoutes';

const POPULAR_KEYWORDS = ['토너', '선크림', '마스크팩', '비타민', '샴푸', '립밤'];

function SearchResults() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const keywordParam = searchParams.get('keyword') || '';
  const currentPage = parseInt(searchParams.get('page') || '0', 10);
  const [keywordInput, setKeywordInput] = useState(keywordParam);
  const [productsPage, setProductsPage] = useState<PageData | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    setKeywordInput(keywordParam);
  }, [keywordParam]);

  useEffect(() => {
    if (!keywordParam) {
      setProductsPage(null);
      return;
    }

    const loadProducts = async () => {
      setIsLoading(true);
      try {
        const result = await fetchProducts({
          page: currentPage,
          keyword: keywordParam,
          category: ALL_CATEGORY_LABEL,
        });
        setProductsPage(result);
      } catch (error) {
        console.error('검색 결과 로딩 실패', error);
      } finally {
        setIsLoading(false);
      }
    };

    loadProducts();
  }, [currentPage, keywordParam]);

  const submitSearch = useCallback((event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const path = buildSearchResultsPath(keywordInput);
    if (path) {
      navigate(path);
    }
  }, [keywordInput, navigate]);

  const selectPopularKeyword = useCallback((keyword: string) => {
    const path = buildSearchResultsPath(keyword);
    if (path) {
      navigate(path);
    }
  }, [navigate]);

  const changePage = useCallback((pageIndex: number) => {
    const nextParams = new URLSearchParams(searchParams);
    nextParams.set('page', pageIndex.toString());
    setSearchParams(nextParams);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }, [searchParams, setSearchParams]);

  return (
    <div className="search-results-page">
      <section className="search-results-panel">
        <form className="search-results-form" onSubmit={submitSearch}>
          <Search size={24} strokeWidth={2.2} />
          <input
            type="text"
            value={keywordInput}
            onChange={(event) => setKeywordInput(event.target.value)}
            placeholder="상품명 또는 브랜드 검색"
          />
        </form>

        <div className="popular-keywords">
          <h2>인기 검색어</h2>
          <div className="popular-keyword-list">
            {POPULAR_KEYWORDS.map(keyword => (
              <button
                key={keyword}
                type="button"
                onClick={() => selectPopularKeyword(keyword)}
              >
                {keyword}
              </button>
            ))}
          </div>
        </div>

        {keywordParam && (
          <div className="search-result-summary">
            <strong>{keywordParam}</strong>의 검색 결과{' '}
            <span>{productsPage?.totalElements.toLocaleString() || 0}건</span>
          </div>
        )}
      </section>

      <section className="search-products-section">
        {isLoading && <div className="loading">검색 중...</div>}

        {!isLoading && keywordParam && productsPage?.content.length === 0 && (
          <div className="empty-state">검색 결과가 없습니다.</div>
        )}

        {!isLoading && productsPage && productsPage.content.length > 0 && (
          <>
            <div className="product-grid">
              {productsPage.content.map(product => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
            <Pagination page={productsPage} onPageChange={changePage} />
          </>
        )}
      </section>
    </div>
  );
}

export default SearchResults;
