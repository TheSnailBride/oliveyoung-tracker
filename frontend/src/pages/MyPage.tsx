import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

function MyPage() {
  const [loading, setLoading] = useState(true);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) {
      setIsLoggedIn(false);
      setLoading(false);
      return;
    }
    setIsLoggedIn(true);
    setLoading(false);
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('token');
    navigate('/');
  };

  if (loading) return <div className="loading-state">불러오는 중...</div>;

  if (!isLoggedIn) {
    return (
      <div className="mypage-container" style={{ padding: '20px', textAlign: 'center', marginTop: '100px' }}>
        <p style={{ marginBottom: '20px' }}>로그인이 필요합니다.</p>
        <button
          onClick={() => navigate('/login')}
          style={{ padding: '12px 24px', background: '#111', color: '#fff', border: 'none', borderRadius: '8px', fontWeight: 'bold', cursor: 'pointer' }}
        >
          로그인 하러가기
        </button>
      </div>
    );
  }

  return (
    <div className="mypage-container" style={{ padding: '20px' }}>
      <h1 style={{ fontSize: '20px', fontWeight: 'bold', marginBottom: '20px' }}>마이페이지</h1>
      <button 
        onClick={handleLogout}
        style={{ padding: '12px 24px', background: '#ff4b4b', color: '#fff', border: 'none', borderRadius: '8px', fontWeight: 'bold', cursor: 'pointer', width: '100%' }}
      >
        로그아웃
      </button>
    </div>
  );
}

export default MyPage;