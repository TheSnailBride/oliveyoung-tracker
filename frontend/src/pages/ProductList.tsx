import { useCallback, useEffect, useState } from 'react';

import {
  fetchAtLowestProducts,
  fetchProductStats,
  fetchProducts,
  fetchTopDiscountedProducts,
  type PageData,
  type Product,
  type Stats,
} from '../api/products';
import CategoryFilter from '../components/CategoryFilter';
import Pagination from '../components/Pagination';
import ProductCard from '../components/ProductCard';
import ProductRail from '../components/ProductRail';
import { useProductFilters } from '../hooks/useProductFilters';

function ProductList() {
  const [lowestProducts, setLowestProducts] = useState<Product[]>([]);
  const [topDiscounted, setTopDiscounted] = useState<Product[]>([]);
  const [allProductsPage, setAllProductsPage] = useState<PageData | null>(null);
  const [stats, setStats] = useState<Stats>({ total: 0, onSale: 0, atLowest: 0 });

  const {
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
  } = useProductFilters();

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

  const fetchAllProducts = useCallback(async () => {
    try {
      const productPage = await fetchProducts({
        page: currentPage,
        keyword: keywordParam,
        category: categoryParam,
        selectedGroup,
      });

      setAllProductsPage(productPage);
    } catch (err) {
      console.error(err);
    }
  }, [categoryParam, currentPage, keywordParam, selectedGroup]);

  useEffect(() => {
    fetchInitialData();
  }, [fetchInitialData]);

  useEffect(() => {
    fetchAllProducts();
  }, [fetchAllProducts]);

  return (
    <>
      <section className="hero">
        <h1>올리브영 가격 추적기</h1>
        <p>세일 기간 "가격 장난질"에 속지 마세요.</p>

        <form className="hero-search" onSubmit={submitSearch}>
          <div className="search-input-wrap">
            <i className="fa-solid fa-magnifying-glass search-icon"></i>
            <input
              type="text"
              placeholder="상품명 또는 브랜드 검색"
              value={keywordInput}
              onChange={(event) => setKeywordInput(event.target.value)}
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
          showOriginalPrice
        />

        <section className="home-section" id="all-products-section">
          <div className="section-header" style={{ marginBottom: '10px' }}>
            <h2 className="section-title">✨ 전체 상품</h2>
          </div>

          <CategoryFilter
            category={categoryParam}
            selectedGroup={selectedGroup}
            onGroupChange={changeGroup}
            onCategoryChange={changeCategory}
          />

          <div className="product-grid">
            {allProductsPage?.content.map(product => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>

          {allProductsPage && (
            <Pagination page={allProductsPage} onPageChange={changePage} />
          )}
        </section>
      </div>
    </>
  );
}

export default ProductList;
