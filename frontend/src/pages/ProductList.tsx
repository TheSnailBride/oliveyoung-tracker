import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import axios from 'axios';

interface Product {
  id: number;
  name: string;
  brand: string;
  currentPrice: number;
  originalPrice: number;
  discountRate: number;
  imageUrl: string;
  isSale: boolean;
}

interface PageData {
  content: Product[];
  totalPages: number;
  number: number;
  totalElements: number;
}

interface Stats {
  total: number;
  onSale: number;
  atLowest: number;
}

function ProductList() {
  const [lowestProducts, setLowestProducts] = useState<Product[]>([]);
  const [topDiscounted, setTopDiscounted] = useState<Product[]>([]);
  const [allProductsPage, setAllProductsPage] = useState<PageData | null>(null);
  const [stats, setStats] = useState<Stats>({ total: 0, onSale: 0, atLowest: 0 });
  const [currentPage, setCurrentPage] = useState(0);
  const [keyword, setKeyword] = useState('');

  const fetchInitialData = useCallback(async () => {
    try {
      const [statRes, lowestRes, topRes] = await Promise.all([
        axios.get('/api/products/stats'),
        axios.get('/api/products/at-lowest?size=10'),
        axios.get('/api/products/top-discounted?size=10')
      ]);
      setStats(statRes.data.data);
      setLowestProducts(lowestRes.data.data);
      setTopDiscounted(topRes.data.data.content);
    } catch (err) { console.error(err); }
  }, []);

  const fetchAllProducts = useCallback(async () => {
    try {
      const res = await axios.get('/api/products', {
        params: { page: currentPage, size: 20, keyword: keyword || undefined, sort: 'updatedAt,desc' }
      });
      setAllProductsPage(res.data.data);
    } catch (err) { console.error(err); }
  }, [currentPage, keyword]);

  useEffect(() => { fetchInitialData(); }, [fetchInitialData]);
  useEffect(() => { fetchAllProducts(); }, [fetchAllProducts]);

  return (
    <>
      <section className="hero">
        <h1>올리브영 가격 추적기</h1>
        <p>세일 기간 "가격 장난질"에 속지 마세요.</p>
        
        <form className="hero-search" onSubmit={(e) => { e.preventDefault(); setCurrentPage(0); fetchAllProducts(); }}>
          <div className="search-input-wrap">
            <i className="fa-solid fa-magnifying-glass search-icon"></i>
            <input 
              type="text" 
              placeholder="상품명 또는 브랜드 검색" 
              value={keyword} 
              onChange={(e) => setKeyword(e.target.value)} 
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
        <section className="home-section">
          <div className="section-header">
            <h2 className="section-title">🏆 역대 최저가</h2>
            <span className="section-desc">지금 바로 확인</span>
          </div>
          <div className="scroll-row">
            {lowestProducts.map(p => (
              <Link to={`/product/${p.id}`} key={p.id} className="scroll-card">
                <div className="scroll-card-img">
                  <img src={p.imageUrl} alt={p.name} loading="lazy" />
                </div>
                <div className="scroll-card-body">
                  <p className="card-name">{p.name}</p>
                  <p className="price-current">{p.currentPrice.toLocaleString()}원</p>
                </div>
              </Link>
            ))}
          </div>
        </section>

        <section className="home-section">
          <div className="section-header">
            <h2 className="section-title">🔥 오늘의 대란템</h2>
            <span className="section-desc">할인율 Top 10</span>
          </div>
          <div className="scroll-row">
            {topDiscounted.map(p => (
              <Link to={`/product/${p.id}`} key={p.id} className="scroll-card">
                <div className="scroll-card-img">
                  <span className="badge-discount">{p.discountRate}%</span>
                  <img src={p.imageUrl} alt={p.name} loading="lazy" />
                </div>
                <div className="scroll-card-body">
                  <p className="card-name">{p.name}</p>
                  <p className="price-current">{p.currentPrice.toLocaleString()}원</p>
                  <p className="price-original" style={{ textDecoration: 'line-through', color: '#999', fontSize: '0.8rem' }}>
                    {p.originalPrice.toLocaleString()}원
                  </p>
                </div>
              </Link>
            ))}
          </div>
        </section>

        <section className="home-section">
          <div className="section-header">
            <h2 className="section-title">✨ 실시간 인기 상품</h2>
          </div>
          <div className="product-grid">
            {allProductsPage?.content.map(p => (
              <Link to={`/product/${p.id}`} key={p.id} className="product-card">
                <div className="card-image">
                  {p.isSale && <span className="badge-discount">{p.discountRate}%</span>}
                  <img src={p.imageUrl} alt={p.name} loading="lazy" />
                </div>
                <div className="card-body">
                  <p className="card-brand">{p.brand}</p>
                  <h3 className="card-name">{p.name}</h3>
                  <p className="price-current">{p.currentPrice.toLocaleString()}원</p>
                </div>
              </Link>
            ))}
          </div>

          {allProductsPage && allProductsPage.totalPages > 1 && (
            <div className="pagination">
              {Array.from({ length: Math.min(5, allProductsPage.totalPages) }, (_, i) => {
                const start = Math.max(0, Math.min(allProductsPage.totalPages - 5, allProductsPage.number - 2));
                const idx = start + i;
                if (idx < 0 || idx >= allProductsPage.totalPages) return null;
                return (
                  <button key={idx} className={allProductsPage.number === idx ? 'active' : ''} onClick={() => {setCurrentPage(idx); window.scrollTo({top: 400, behavior: 'smooth'})}}>
                    {idx + 1}
                  </button>
                );
              })}
            </div>
          )}
        </section>
      </div>
    </>
  );
}

export default ProductList;
