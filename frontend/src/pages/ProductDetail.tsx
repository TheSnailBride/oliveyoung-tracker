import { useState, useEffect, type FormEvent } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { Line } from 'react-chartjs-2';
import annotationPlugin from 'chartjs-plugin-annotation';
import {
  createProductAlertRequest,
  normalizeProductAlertResponse,
  type ProductAlertData
} from '../api/productAlerts';
import {
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
import { Bell, BellRing } from 'lucide-react';

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

const DEFAULT_PRODUCT_ALERT: ProductAlertData = {
  isAlertSet: false,
  targetPrice: null,
};

function ProductDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [product, setProduct] = useState<ProductDetailData | null>(null);
  const [history, setHistory] = useState<PriceHistory[]>([]);
  const [days, setDays] = useState(30);
  const [productAlert, setProductAlert] = useState<ProductAlertData>(DEFAULT_PRODUCT_ALERT);
  const [isAlertModalOpen, setIsAlertModalOpen] = useState(false);
  const [alertPriceInput, setAlertPriceInput] = useState('');
  const [alertFormError, setAlertFormError] = useState('');
  const [alertFeedback, setAlertFeedback] = useState('');
  const [savingAlert, setSavingAlert] = useState(false);
  const [similarProducts, setSimilarProducts] = useState<Product[]>([]);
  const [loadingSimilar, setLoadingSimilar] = useState<boolean>(true);

  useEffect(() => {
    if (!id) return;

    window.scrollTo(0, 0);
    const fetchData = async () => {
      try {
        const token = localStorage.getItem('token');
        const headers = token ? { Authorization: `Bearer ${token}` } : {};

        const [productData, historyData, alertRes] = await Promise.all([
          fetchProductDetail(id),
          fetchPriceHistory(id, days),
          token ? axios.get(`/api/products/${id}/alert`, { headers }) : Promise.resolve({ data: { data: DEFAULT_PRODUCT_ALERT } })
        ]);
        setProduct(productData);
        setHistory(historyData);

        const alertData = normalizeProductAlertResponse(alertRes.data.data);
        setProductAlert(alertData);
        setAlertPriceInput(alertData.targetPrice ? String(alertData.targetPrice) : '');
        setAlertFormError('');
        setAlertFeedback('');
      } catch (err) {
        console.error('데이터 로딩 실패', err);
      }
    };
    fetchData();
  }, [id, days]);

  useEffect(() => {
    if (!id) return;

    const loadSimilarProducts = async () => {
      setLoadingSimilar(true);
      try {
        setSimilarProducts(await fetchSimilarProducts(id));
      } catch (error) {
        console.error('Failed to fetch similar products:', error);
      } finally {
        setLoadingSimilar(false);
      }
    };

    loadSimilarProducts();
  }, [id]);

  const openAlertModal = () => {
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/login');
      return;
    }

    setAlertPriceInput(productAlert.targetPrice ? String(productAlert.targetPrice) : '');
    setAlertFormError('');
    setAlertFeedback('');
    setIsAlertModalOpen(true);
  };

  const closeAlertModal = () => {
    if (savingAlert) return;
    setIsAlertModalOpen(false);
    setAlertFormError('');
  };

  const submitAlert = async (event: FormEvent) => {
    event.preventDefault();

    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/login');
      return;
    }

    const parsedPrice = parseInt(alertPriceInput.replace(/[^0-9]/g, ''), 10);
    if (Number.isNaN(parsedPrice) || parsedPrice <= 0) {
      setAlertFormError('목표가를 숫자로 입력해주세요.');
      return;
    }

    setSavingAlert(true);
    setAlertFormError('');
    try {
      const res = await axios.post(
        `/api/products/${id}/alert`,
        createProductAlertRequest(parsedPrice),
        { headers: { Authorization: `Bearer ${token}` } }
      );
      const nextAlert = normalizeProductAlertResponse(res.data.data);
      setProductAlert(nextAlert);
      setAlertPriceInput(nextAlert.targetPrice ? String(nextAlert.targetPrice) : '');
      setIsAlertModalOpen(false);
      setAlertFeedback(nextAlert.targetPrice
        ? `${nextAlert.targetPrice.toLocaleString()}원 이하가 되면 알려드릴게요.`
        : '목표가 알림이 해제되었습니다.');
    } catch (err) {
      console.error('알림 설정 실패', err);
      setAlertFormError('알림 설정 중 오류가 발생했습니다.');
    } finally {
      setSavingAlert(false);
    }
  };

  const clearAlert = async () => {
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/login');
      return;
    }

    setSavingAlert(true);
    setAlertFormError('');
    try {
      const res = await axios.post(
        `/api/products/${id}/alert`,
        createProductAlertRequest(null),
        { headers: { Authorization: `Bearer ${token}` } }
      );
      const nextAlert = normalizeProductAlertResponse(res.data.data);
      setProductAlert(nextAlert);
      setAlertPriceInput('');
      setIsAlertModalOpen(false);
      setAlertFeedback('목표가 알림이 해제되었습니다.');
    } catch (err) {
      console.error('알림 해제 실패', err);
      setAlertFormError('알림 해제 중 오류가 발생했습니다.');
    } finally {
      setSavingAlert(false);
    }
  };

  if (!product) return <div className="loading">불러오는 중...</div>;

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
          <div className="chart-period">
            <button className={`period-btn ${days === 30 ? 'active' : ''}`} onClick={() => setDays(30)}>1개월</button>
            <button className={`period-btn ${days === 90 ? 'active' : ''}`} onClick={() => setDays(90)}>3개월</button>
          </div>
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

      {alertFeedback && (
        <div className="detail-toast" role="status">
          {alertFeedback}
        </div>
      )}

      {isAlertModalOpen && product && (
        <div className="alert-modal-backdrop" role="dialog" aria-modal="true">
          <form className="alert-modal" onSubmit={submitAlert}>
            <div className="alert-modal-header">
              <h2>목표가 알림 설정</h2>
              <button type="button" className="alert-modal-close" onClick={closeAlertModal} disabled={savingAlert}>
                ×
              </button>
            </div>
            <div className="alert-modal-body">
              <div className="alert-current-price">
                <span>현재가</span>
                <strong>{product.currentPrice.toLocaleString()}원</strong>
              </div>
              <label className="alert-price-field">
                <span>목표가</span>
                <input
                  type="text"
                  inputMode="numeric"
                  value={alertPriceInput}
                  onChange={(event) => setAlertPriceInput(event.target.value)}
                  placeholder="예: 12000"
                  disabled={savingAlert}
                />
              </label>
              {alertFormError && <p className="alert-form-error">{alertFormError}</p>}
            </div>
            <div className="alert-modal-actions">
              {productAlert.isAlertSet && (
                <button type="button" className="alert-clear-btn" onClick={clearAlert} disabled={savingAlert}>
                  알림 해제
                </button>
              )}
              <button type="submit" className="alert-save-btn" disabled={savingAlert}>
                {savingAlert ? '저장 중...' : productAlert.isAlertSet ? '목표가 변경' : '알림 설정'}
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="detail-bottom-fixed detail-actions">
        <button
          onClick={openAlertModal}
          className={`btn-alert-bottom ${productAlert.isAlertSet ? 'active' : ''}`}
        >
          <div className="btn-alert-main">
            {productAlert.isAlertSet ? <BellRing size={20} /> : <Bell size={20} />}
            <span>{productAlert.isAlertSet ? '목표가 알림 중' : '목표가 알림'}</span>
          </div>
          {productAlert.isAlertSet && productAlert.targetPrice && (
            <span className="btn-alert-sub">({productAlert.targetPrice.toLocaleString()}원 이하)</span>
          )}
        </button>
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
