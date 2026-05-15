import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { Line } from 'react-chartjs-2';
import annotationPlugin from 'chartjs-plugin-annotation';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler
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

interface ProductDetailData {
  id: number;
  name: string;
  brand: string;
  currentPrice: number;
  originalPrice: number;
  discountRate: number;
  imageUrl: string;
  productUrl: string;
  lowestPrice: number;
  highestPrice: number;
}

interface PriceHistory {
  currentPrice: number;
  recordedAt: string;
}

interface Product {
  id: number;
  name: string;
  brand: string;
  imageUrl: string;
  currentPrice: number;
  originalPrice: number;
  discountRate: number;
  isSale: boolean;
}

function ProductDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [product, setProduct] = useState<ProductDetailData | null>(null);
  const [history, setHistory] = useState<PriceHistory[]>([]);
  const [days, setDays] = useState(30);
  const [isAlertSet, setIsAlertSet] = useState(false);
  const [targetPrice, setTargetPrice] = useState<number | null>(null);
  const [similarProducts, setSimilarProducts] = useState<Product[]>([]);
  const [loadingSimilar, setLoadingSimilar] = useState<boolean>(true);

  useEffect(() => {
    window.scrollTo(0, 0);
    const fetchData = async () => {
      try {
        const token = localStorage.getItem('token');
        const headers = token ? { Authorization: `Bearer ${token}` } : {};
        
        const [prodRes, historyRes, alertRes] = await Promise.all([
          axios.get(`/api/products/${id}`),
          axios.get(`/api/products/${id}/prices?days=${days}`),
          token ? axios.get(`/api/products/${id}/alert`, { headers }) : Promise.resolve({ data: { data: { isAlertSet: false, targetPrice: -1 } } })
        ]);
        setProduct(prodRes.data.data);
        setHistory(historyRes.data.data);
        
        const alertData = alertRes.data.data;
        setIsAlertSet(alertData.isAlertSet);
        if (alertData.targetPrice && alertData.targetPrice !== -1) {
          setTargetPrice(alertData.targetPrice);
        } else {
          setTargetPrice(null);
        }
      } catch (err) {
        console.error('데이터 로딩 실패', err);
      }
    };
    fetchData();
  }, [id, days]);

  useEffect(() => {
    if (!id) return;
    
    const fetchSimilarProducts = async () => {
      setLoadingSimilar(true);
      try {
        const response = await axios.get(`/api/v1/products/${id}/similar`);
        if (response.data.success) {
          setSimilarProducts(response.data.data);
        }
      } catch (error) {
        console.error('Failed to fetch similar products:', error);
      } finally {
        setLoadingSimilar(false);
      }
    };

    fetchSimilarProducts();
  }, [id]);

  const toggleAlert = async () => {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        alert('로그인이 필요합니다.');
        navigate('/login');
        return;
      }

      let reqBody = {};

      if (isAlertSet) {
        const confirmCancel = window.confirm('알림 설정을 해제하시겠습니까?');
        if (!confirmCancel) return;
        // targetPrice 없이 보내면 해제
      } else {
        const priceStr = window.prompt(`현재 가격은 ${product?.currentPrice.toLocaleString()}원 입니다.\n얼마 이하로 떨어지면 알림을 받을까요? (숫자만 입력)`, '');
        if (priceStr === null) return; // 취소 누름
        const parsedPrice = parseInt(priceStr.replace(/[^0-9]/g, ''));
        if (isNaN(parsedPrice) || parsedPrice <= 0) {
          alert('올바른 금액을 입력해주세요.');
          return;
        }
        reqBody = { targetPrice: parsedPrice };
      }
      
      const res = await axios.post(`/api/products/${id}/alert`, reqBody, {
        headers: { Authorization: `Bearer ${token}` }
      });
      
      const resultData = res.data.data;
      setIsAlertSet(resultData.isAlertSet);
      
      if (resultData.isAlertSet) {
        setTargetPrice(resultData.targetPrice);
        alert(`가격이 ${resultData.targetPrice.toLocaleString()}원 이하로 내려가면 알려드릴게요!`);
      } else {
        setTargetPrice(null);
        alert('알림이 해제되었습니다.');
      }
    } catch (err) {
      console.error('알림 설정 실패', err);
      alert('알림 설정 중 오류가 발생했습니다.');
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

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: { mode: 'index' as const, intersect: false },
    plugins: {
      legend: { display: false },
      tooltip: {
        enabled: true,
        backgroundColor: '#111',
        padding: 12,
        callbacks: { label: (ctx: any) => `${ctx.parsed.y.toLocaleString()}원` }
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
            <div className="purchase-tip" style={{ 
              padding: '5px 10px', 
              background: priceDiff > 0 ? '#f8f9fa' : '#f2f8f2', 
              color: priceDiff > 0 ? '#666' : '#3D8B37', 
              borderRadius: '8px', 
              fontSize: '12px', 
              fontWeight: '800' 
            }}>
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

      <div className="detail-bottom-fixed" style={{ display: 'flex', gap: '10px' }}>
        <button
          onClick={toggleAlert}
          style={{
            flex: 1,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexDirection: 'column',
            gap: '2px',
            backgroundColor: isAlertSet ? '#f2f8f2' : '#fff',
            color: isAlertSet ? '#3D8B37' : '#333',
            border: `1px solid ${isAlertSet ? '#3D8B37' : '#ddd'}`,
            borderRadius: '12px',
            cursor: 'pointer',
            padding: '4px'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontWeight: '800', fontSize: '15px' }}>
            {isAlertSet ? <BellRing size={20} /> : <Bell size={20} />}
            <span>{isAlertSet ? '목표가 알림 중' : '목표가 알림'}</span>
          </div>
          {isAlertSet && targetPrice && (
            <span style={{ fontSize: '11px', fontWeight: 'bold' }}>({targetPrice.toLocaleString()}원 이하)</span>
          )}
        </button>
        <a 
          href={product.productUrl} 
          target="_blank" 
          rel="noreferrer" 
          className="btn-buy-bottom"
          style={{ flex: 1.5, margin: 0 }}
        >
          공식몰에서 확인
        </a>
      </div>
    </div>
  );
}

export default ProductDetail;
