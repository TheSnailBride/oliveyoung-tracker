import { useState, useEffect, type MouseEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import {
  NotificationCenterData,
  normalizeNotificationCenterResponse,
  removeNotificationFromCenter
} from '../api/notifications';

const EMPTY_NOTIFICATION_CENTER: NotificationCenterData = {
  notifications: [],
  activeAlerts: [],
};

function formatDateTime(createdAt: string) {
  const date = new Date(createdAt);
  return `${date.getMonth() + 1}월 ${date.getDate()}일 ${date.getHours()}:${date.getMinutes().toString().padStart(2, '0')}`;
}

function formatPrice(price: number | null | undefined) {
  return price == null ? '-' : `${price.toLocaleString()}원`;
}

function Alerts() {
  const [notificationCenter, setNotificationCenter] = useState<NotificationCenterData>(EMPTY_NOTIFICATION_CENTER);
  const [loading, setLoading] = useState(true);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [deletingNotificationId, setDeletingNotificationId] = useState<number | null>(null);
  const [notificationError, setNotificationError] = useState('');
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
        setNotificationCenter(normalizeNotificationCenterResponse(res.data.data));
        setNotificationError('');

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

  const deleteNotification = async (event: MouseEvent, notificationId: number) => {
    event.stopPropagation();
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/login');
      return;
    }

    setDeletingNotificationId(notificationId);
    setNotificationError('');
    try {
      await axios.delete(`/api/notifications/${notificationId}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setNotificationCenter(current => removeNotificationFromCenter(current, notificationId));
    } catch (err) {
      console.error('알림 삭제 실패', err);
      setNotificationError('알림을 삭제하지 못했습니다.');
    } finally {
      setDeletingNotificationId(null);
    }
  };

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

  const { notifications, activeAlerts } = notificationCenter;
  const hasNotificationCenterItems = notifications.length > 0 || activeAlerts.length > 0;

  return (
    <div className="alerts-page">
      <div className="alerts-header">
        <h1 className="title">알림 센터</h1>
      </div>

      <div className="alerts-container">
        {notificationError && <div className="alerts-error">{notificationError}</div>}

        {!hasNotificationCenterItems ? (
          <div className="empty-alerts">
            <div className="icon">🔔</div>
            <p className="main-msg">새로운 알림이 없습니다.</p>
            <p className="sub-msg">관심 상품을 등록하고<br/>최저가 알림을 받아보세요!</p>
          </div>
        ) : (
          <>
            {activeAlerts.length > 0 && (
              <section className="alerts-section">
                <h2 className="alerts-section-title">진행 중인 목표가 알림</h2>
                <div className="alerts-list">
                  {activeAlerts.map(alert => (
                    <div
                      key={alert.id}
                      className="alert-item active-alert-item"
                      onClick={() => navigate(`/product/${alert.productId}`)}
                    >
                      <div className="item-img">
                        <img src={alert.productImageUrl} alt="" />
                      </div>
                      <div className="item-content">
                        <div className="item-time">{formatDateTime(alert.createdAt)} 등록</div>
                        <div className="item-title">{alert.productName}</div>
                        <div className="item-msg active-alert-msg">
                          목표가까지 기다리는 중입니다.
                        </div>
                        <div className="item-price">
                          목표가: <span className="price">{formatPrice(alert.targetPrice)}</span>
                          <span className="price-divider">현재가: {formatPrice(alert.currentPrice)}</span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </section>
            )}

            {notifications.length > 0 && (
              <section className="alerts-section">
                <h2 className="alerts-section-title">도착한 알림</h2>
                <div className="alerts-list">
                  {notifications.map(noti => (
                    <div
                      key={noti.id}
                      className={`alert-item ${noti.isRead ? 'read' : 'unread'}`}
                      onClick={() => navigate(`/product/${noti.productId}`)}
                    >
                      <div className="item-img">
                        <img src={noti.productImageUrl} alt="" />
                      </div>
                      <div className="item-content">
                        <div className="item-time">{formatDateTime(noti.createdAt)}</div>
                        <div className="item-title">{noti.productName}</div>
                        <div className="item-msg">
                          설정한 목표가에 도달하여 알림이 발송되었습니다!
                        </div>
                        <div className="item-price">
                          알림가: <span className="price">{formatPrice(noti.priceAtAlert)}</span>
                        </div>
                      </div>
                      {!noti.isRead && <div className="unread-dot"></div>}
                      <button
                        type="button"
                        className="notification-delete-btn"
                        onClick={(event) => deleteNotification(event, noti.id)}
                        disabled={deletingNotificationId === noti.id}
                        aria-label={`${noti.productName} 알림 삭제`}
                      >
                        삭제
                      </button>
                    </div>
                  ))}
                </div>
              </section>
            )}
          </>
        )}
      </div>
    </div>
  );
}

export default Alerts;
