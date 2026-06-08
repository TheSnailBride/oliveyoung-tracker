import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';

import {
  fetchAtLowestProducts,
  fetchPriceDroppedProducts,
  fetchProductStats,
  fetchTopDiscountedProducts,
  type Product,
  type Stats,
} from '../api/products';
import ProductCard from '../components/ProductCard';
import ProductRail from '../components/ProductRail';
import { ALL_CATEGORY_LABEL, CATEGORY_GROUPS, getCategoriesParamForGroup } from '../constants/categories';
import { buildSearchResultsPath } from '../utils/searchRoutes';

const HOME_PRICE_DROP_SIZE = 9;

function ProductList() {
  const navigate = useNavigate();
  const [lowestProducts, setLowestProducts] = useState<Product[]>([]);
  const [topDiscounted, setTopDiscounted] = useState<Product[]>([]);
  const [priceDroppedProducts, setPriceDroppedProducts] = useState<Product[]>([]);
  const [selectedCategoryGroup, setSelectedCategoryGroup] = useState('');
  const [loadingPriceDrops, setLoadingPriceDrops] = useState(false);
  const [stats, setStats] = useState<Stats>({ total: 0, onSale: 0, atLowest: 0 });
  const [heroKeyword, setHeroKeyword] = useState('');
  const priceDropRequestId = useRef(0);

  const fetchInitialData = useCallback(async () => {
    try {
      const [statData, lowestData, topDiscountedData] = await Promise.all([
        fetchProductStats(),
        fetchAtLowestProducts(10),
        fetchTopDiscountedProducts(10),
      ]);

      setStats(statData);
      setLowestProducts(lowestData);
      setTopDiscounted(topDiscountedData);
    } catch (err) {
      console.error(err);
    }
  }, []);

  const fetchPriceDrops = useCallback(async () => {
    const requestId = priceDropRequestId.current + 1;
    priceDropRequestId.current = requestId;
    const selectedGroup = CATEGORY_GROUPS.find(group => group.name === selectedCategoryGroup);
    const categories = getCategoriesParamForGroup(selectedGroup);

    setLoadingPriceDrops(true);
    try {
      const products = await fetchPriceDroppedProducts(categories, HOME_PRICE_DROP_SIZE);

      if (requestId === priceDropRequestId.current) {
        setPriceDroppedProducts(products);
      }
    } catch (err) {
      console.error(err);
    } finally {
      if (requestId === priceDropRequestId.current) {
        setLoadingPriceDrops(false);
      }
    }
  }, [selectedCategoryGroup]);

  const submitSearch = useCallback((event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const path = buildSearchResultsPath(heroKeyword);
    if (path) {
      navigate(path);
    }
  }, [heroKeyword, navigate]);

  useEffect(() => {
    fetchInitialData();
  }, [fetchInitialData]);

  useEffect(() => {
    fetchPriceDrops();
  }, [fetchPriceDrops]);

  return (
    <>
      <section className="hero">
        <h1>한 번만 확인해도 달라지는 쇼핑 습관</h1>
        <p>가격하락 상품을 카테고리별로 확인해보세요</p>

        <form className="hero-search" onSubmit={submitSearch}>
          <div className="search-input-wrap">
            <i className="fa-solid fa-magnifying-glass search-icon"></i>
            <input
              type="text"
              placeholder="상품명 또는 브랜드 검색"
              value={heroKeyword}
              onChange={(event) => setHeroKeyword(event.target.value)}
            />
          </div>
          <button type="submit">검색</button>
        </form>

        <div className="hero-stats">
          <div className="stat-card"><span>{stats.total.toLocaleString()}</span><small>추적중</small></div>
          <div className="stat-card"><span>{stats.onSale.toLocaleString()}</span><small>세일중</small></div>
          <div className="stat-card"><span>{stats.atLowest.toLocaleString()}</span><small>최저가</small></div>
        </div>
      </section>

      <div className="container">
        <ProductRail
          title="🏆 역대 최저가"
          description="지금 바로 확인"
          products={lowestProducts}
        />

        <ProductRail
          title="🔥 오늘의 대란템"
          description="할인율 Top 10"
          products={topDiscounted}
        />

        <section className="home-section" id="price-drop-section">
          <div className="section-header" style={{ marginBottom: '10px' }}>
            <h2 className="section-title">📉 카테고리별 가격하락 상품</h2>
            <span className="section-desc">대분류를 눌러 확인</span>
          </div>

          <div className="home-category-tabs">
            <button
              type="button"
              onClick={() => setSelectedCategoryGroup('')}
              className={`home-category-tab ${selectedCategoryGroup === '' ? 'active' : ''}`}
            >
              {ALL_CATEGORY_LABEL}
            </button>
            {CATEGORY_GROUPS.map(group => (
              <button
                type="button"
                key={group.name}
                onClick={() => setSelectedCategoryGroup(group.name)}
                className={`home-category-tab ${selectedCategoryGroup === group.name ? 'active' : ''}`}
              >
                {group.name}
              </button>
            ))}
          </div>

          <div className="price-drop-content" aria-busy={loadingPriceDrops}>
            {loadingPriceDrops && priceDroppedProducts.length === 0 && (
              <div className="loading">가격하락 상품을 불러오는 중...</div>
            )}

            {!loadingPriceDrops && priceDroppedProducts.length === 0 && (
              <div className="empty-state">아직 보여줄 가격하락 상품이 없습니다.</div>
            )}

            {priceDroppedProducts.length > 0 && (
              <div className={`home-price-drop-grid ${loadingPriceDrops ? 'is-loading' : ''}`}>
                {priceDroppedProducts.map(product => (
                  <ProductCard key={product.id} product={product} />
                ))}
              </div>
            )}

            {loadingPriceDrops && priceDroppedProducts.length > 0 && (
              <div className="price-drop-loading-pill">불러오는 중...</div>
            )}
          </div>
        </section>
      </div>
    </>
  );
}

export default ProductList;
