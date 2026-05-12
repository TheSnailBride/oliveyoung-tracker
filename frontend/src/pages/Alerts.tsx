import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

interface NotificationData {
  id: number;
  productId: number;
  productName: string;
  productImageUrl: string;
  priceAtAlert: number;
  isRead: boolean;
  createdAt: string;
}

function Alerts() {
  const [notifications, setNotifications] = useState<NotificationData[]>([]);
  const [loading, setLoading] = useState(true);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchNotifications = async () => {
      const token = localStorage.getItem('token');
      if (!token) {
        setIsLoggedIn(false);
        setLoading(false);
        return;
      }
      setIsLoggedIn(true);

      try {
        const res = await axios.get('/api/notifications', {
          headers: { Authorization: `Bearer ${token}` }
        });
        setNotifications(res.data.data);

        axios.post('/api/notifications/read', {}, {
          headers: { Authorization: `Bearer ${token}` }
        }).catch(e => console.error("읽음 처리 실패", e));
      } catch (err: any) {
        if (err.response && (err.response.status === 401 || err.response.status === 403)) {
          localStorage.removeItem('token');
          setIsLoggedIn(false);
        }
        console.error('알림 로딩 실패', err);
      } finally {
        setLoading(false);
      }
    };
    fetchNotifications();
  }, []);

  if (loading) return <div className="loading-state">불러오는 중...</div>;

  if (!isLoggedIn) {
    return (
      <div className="alerts-page">
        <div className="alerts-header">
          <h1 className="title">알림 센터</h1>
        </div>
        <div className="empty-alerts">
          <div className="icon">🔒</div>
          <p className="main-msg">로그인이 필요합니다</p>
          <p className="sub-msg" style={{ marginBottom: '20px' }}>
            나만의 최저가 알림 내역을 확인하려면<br/>먼저 로그인해 주세요.
          </p>
          <button
            onClick={() => navigate('/login')}
            style={{ padding: '12px 24px', background: '#111', color: '#fff', border: 'none', borderRadius: '8px', fontWeight: 'bold', cursor: 'pointer' }}
          >
            로그인 하러가기
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="alerts-page">
      <div className="alerts-header">
        <h1 className="title">알림 센터</h1>
      </div>

      <div className="alerts-container">
        {notifications.length === 0 ? (
          <div className="empty-alerts">
            <div className="icon">🔔</div>
            <p className="main-msg">새로운 알림이 없습니다.</p>
            <p className="sub-msg">관심 상품을 등록하고<br/>최저가 알림을 받아보세요!</p>
          </div>
        ) : (
          <div className="alerts-list">
            {notifications.map(noti => {
              const date = new Date(noti.createdAt);
              const timeStr = `${date.getMonth() + 1}월 ${date.getDate()}일 ${date.getHours()}:${date.getMinutes().toString().padStart(2, '0')}`;
              return (
                <div
                  key={noti.id}
                  className={`alert-item ${noti.isRead ? 'read' : 'unread'}`}
                  onClick={() => navigate(`/product/${noti.productId}`)}
                >
                  <div className="item-img">
                    <img src={noti.productImageUrl} alt="" />
                  </div>
                  <div className="item-content">
                    <div className="item-time">{timeStr}</div>
                    <div className="item-title">{noti.productName}</div>
                    <div className="item-msg">
                      설정한 목표가에 도달하여 알림이 발송되었습니다!
                    </div>
                    <div className="item-price">
                      현재가: <span className="price">{noti.priceAtAlert.toLocaleString()}원</span>
                    </div>
                  </div>
                  {!noti.isRead && <div className="unread-dot"></div>}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

export default Alerts;