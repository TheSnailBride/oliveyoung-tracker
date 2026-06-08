import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { Line } from 'react-chartjs-2';
import annotationPlugin from 'chartjs-plugin-annotation';
import {
  DEFAULT_PRICE_HISTORY_DAYS,
  fetchPriceHistory,
  fetchProductDetail,
  fetchSimilarProducts,
  type PriceHistory,
  type Product,
  type ProductDetailData,
} from '../api/products';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler,
  type ChartOptions,
  type TooltipItem
} from 'chart.js';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler,
  annotationPlugin
);

function ProductDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [product, setProduct] = useState<ProductDetailData | null>(null);
  const [history, setHistory] = useState<PriceHistory[]>([]);
  const [similarProducts, setSimilarProducts] = useState<Product[]>([]);
  const [loadingSimilar, setLoadingSimilar] = useState<boolean>(true);
  const [loadingProduct, setLoadingProduct] = useState(true);
  const [productError, setProductError] = useState('');

  useEffect(() => {
    if (!id) {
      setLoadingProduct(false);
      setProductError('상품 정보를 찾을 수 없습니다.');
      return;
    }

    window.scrollTo(0, 0);
    let isCurrent = true;

    const fetchData = async () => {
      setLoadingProduct(true);
      setProductError('');
      setProduct(null);
      setHistory([]);
      setSimilarProducts([]);
      setLoadingSimilar(true);

      try {
        const [productData, historyData] = await Promise.all([
          fetchProductDetail(id),
          fetchPriceHistory(id, DEFAULT_PRICE_HISTORY_DAYS),
        ]);

        if (!isCurrent) return;

        setProduct(productData);
        setHistory(historyData);
      } catch (err) {
        console.error('데이터 로딩 실패', err);
        if (!isCurrent) return;

        setProduct(null);
        setProductError(
          axios.isAxiosError(err) && err.response?.status === 404
            ? '상품을 찾을 수 없습니다.'
            : '상품 정보를 불러오지 못했습니다.'
        );
      } finally {
        if (isCurrent) {
          setLoadingProduct(false);
        }
      }
    };

    fetchData();

    return () => {
      isCurrent = false;
    };
  }, [id]);

  useEffect(() => {
    if (!id || !product) {
      setLoadingSimilar(false);
      return;
    }

    let isCurrent = true;

    const loadSimilarProducts = async () => {
      setLoadingSimilar(true);
      try {
        const products = await fetchSimilarProducts(id);
        if (isCurrent) {
          setSimilarProducts(products);
        }
      } catch (error) {
        console.error('Failed to fetch similar products:', error);
        if (isCurrent) {
          setSimilarProducts([]);
        }
      } finally {
        if (isCurrent) {
          setLoadingSimilar(false);
        }
      }
    };

    loadSimilarProducts();

    return () => {
      isCurrent = false;
    };
  }, [id, product]);

  if (loadingProduct) return <div className="loading">불러오는 중...</div>;

  if (productError || !product) {
    return (
      <section className="page-state">
        <p className="page-state-kicker">상품 상세</p>
        <h1>{productError || '상품 정보를 찾을 수 없습니다.'}</h1>
        <p>상품이 삭제되었거나 일시적으로 정보를 가져오지 못했습니다.</p>
        <button type="button" className="page-state-primary" onClick={() => navigate('/')}>
          홈으로 돌아가기
        </button>
      </section>
    );
  }

  const priceDiff = (product.currentPrice || 0) - (product.lowestPrice || 0);

  const chartData = {
    labels: history.map(h => {
      const d = new Date(h.recordedAt);
      return `${d.getMonth() + 1}/${d.getDate()}`;
    }),
    datasets: [{
      label: '가격',
      data: history.map(h => h.currentPrice),
      borderColor: '#3D8B37',
      backgroundColor: 'rgba(61, 139, 55, 0.05)',
      fill: true,
      tension: 0.4,
      pointRadius: 4,
      pointHoverRadius: 6,
      borderWidth: 2,
    }]
  };

  const chartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: { mode: 'index' as const, intersect: false },
    plugins: {
      legend: { display: false },
      tooltip: {
        enabled: true,
        backgroundColor: '#111',
        padding: 12,
        callbacks: {
          label: (ctx: TooltipItem<'line'>) => {
            const price = ctx.parsed.y;
            return typeof price === 'number' ? `${price.toLocaleString()}원` : '';
          }
        }
      }
    },
    scales: {
      x: { grid: { display: false } },
      y: { display: false }
    }
  };

  return (
    <div className="product-detail-container">
      <div className="detail-top-row">
        <div className="detail-img-side" onClick={() => navigate(-1)}>
          <img src={product.imageUrl} alt={product.name} />
        </div>

        <div className="detail-info-side">
          <p className="detail-brand-pc">{product.brand}</p>
          <h1 className="detail-name-pc">{product.name}</h1>
          <div className="price-and-tip">
            <div className="detail-price-pc">{product.currentPrice.toLocaleString()}원</div>
            <div className={`purchase-tip ${priceDiff > 0 ? 'wait' : 'deal'}`}>
              {priceDiff > 0 ? '👀 존버 추천' : '🔥 득템 찬스'}
            </div>
          </div>
        </div>
      </div>

      <div className="price-summary-card">
        <div className="summary-row">
          <span className="label">현재가</span>
          <span className="value">{product.currentPrice.toLocaleString()}원</span>
        </div>
        <div className="summary-row">
          <span className="label">역대 최저가</span>
          <span className="value low">{product.lowestPrice?.toLocaleString()}원</span>
        </div>
        <div className="summary-row">
          <span className="label">최저가 대비</span>
          <span className="value diff">
            {priceDiff === 0 ? '현재 최저가입니다!' : `${priceDiff.toLocaleString()}원 더 비싸요`}
          </span>
        </div>
      </div>

      <div className="chart-section-pc">
        <div className="chart-header-pc">
          <h2>가격 변동 추이</h2>
        </div>
        <div className="chart-wrap">
          <Line data={chartData} options={chartOptions} />
        </div>
      </div>

      {/* 비슷한 상품 추천 섹션 */}
      {!loadingSimilar && similarProducts.length > 0 && (
        <div className="similar-section">
          <h2>이 브랜드의 비슷한 상품</h2>
          <div className="similar-list">
            {similarProducts.map((prod) => (
              <div
                key={prod.id}
                className="similar-item"
                onClick={() => navigate(`/product/${prod.id}`)}
              >
                <div className="similar-img-wrap">
                  <img src={prod.imageUrl} alt={prod.name} />
                </div>
                <div className="similar-brand">{prod.brand}</div>
                <div className="similar-name">{prod.name}</div>
                <div className="similar-price-wrap">
                  {prod.isSale && (
                    <span className="similar-discount">{prod.discountRate}%</span>
                  )}
                  <span className="similar-price">{prod.currentPrice.toLocaleString()}원</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="detail-bottom-fixed detail-actions">
        <a
          href={product.productUrl}
          target="_blank"
          rel="noreferrer"
          className="btn-buy-bottom"
        >
          공식몰에서 확인
        </a>
      </div>
    </div>
  );
}

export default ProductDetail;
