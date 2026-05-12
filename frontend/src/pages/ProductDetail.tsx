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

function ProductDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [product, setProduct] = useState<ProductDetailData | null>(null);
  const [history, setHistory] = useState<PriceHistory[]>([]);
  const [days, setDays] = useState(30);

  useEffect(() => {
    window.scrollTo(0, 0);
    const fetchData = async () => {
      try {
        const [prodRes, historyRes] = await Promise.all([
          axios.get(`/api/products/${id}`),
          axios.get(`/api/products/${id}/prices?days=${days}`)
        ]);
        setProduct(prodRes.data.data);
        setHistory(historyRes.data.data);
      } catch (err) {
        console.error('데이터 로딩 실패', err);
      }
    };
    fetchData();
  }, [id, days]);

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

      <div className="detail-bottom-fixed">
        <a 
          href={product.productUrl} 
          target="_blank" 
          rel="noreferrer" 
          className="btn-buy-bottom"
        >
          올리브영 공식몰에서 확인하기
        </a>
      </div>
    </div>
  );
}

export default ProductDetail;
