import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

function Login() {
  const navigate = useNavigate();

  // 이미 로그인된 경우 메인으로 리다이렉트
  useEffect(() => {
    if (localStorage.getItem('token')) {
      navigate('/');
    }
  }, [navigate]);

  const handleKakaoLogin = async () => {
    try {
      const res = await axios.get('/api/kakao/auth-url');
      window.location.href = res.data.data.url;
    } catch (err) {
      alert('카카오 로그인 주소를 가져오지 못했습니다.');
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-container">
        <div className="kakao-login-form">
          <div className="auth-logo">🌿</div>
          <h2>로그인</h2>
          <p className="auth-desc">간편하게 로그인하고<br/>원하는 가격이 되면 알림을 받아보세요!</p>
          
          <button className="btn-kakao-login" onClick={handleKakaoLogin}>
            <img src="https://developers.kakao.com/assets/img/lib/logos/kakaolink/kakaolink_btn_medium.png" alt="" />
            <span>카카오로 시작하기</span>
          </button>

          <p className="auth-notice">
            별도의 회원가입 없이<br/>카카오 계정으로 바로 이용 가능합니다.
          </p>
        </div>
      </div>
    </div>
  );
}

export default Login;
