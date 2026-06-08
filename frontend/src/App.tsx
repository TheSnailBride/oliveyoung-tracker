import { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useLocation, useNavigate } from 'react-router-dom';
import ProductList from './pages/ProductList';
import ProductDetail from './pages/ProductDetail';
import SearchResults from './pages/SearchResults';
import Login from './pages/Login';
import MyPage from './pages/MyPage';
import NotFoundPage from './pages/NotFoundPage';
import Header from './components/Header';
import { FEATURES } from './config/features';

function TokenHandler() {
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const token = params.get('token');

    if (token) {
      localStorage.setItem('token', token);
      navigate(location.pathname, { replace: true });
    }
  }, [location, navigate]);

  return null;
}

function App() {
  return (
    <BrowserRouter>
      {FEATURES.kakaoAuth && <TokenHandler />}
      <Header />
      <main style={{ paddingBottom: '80px' }}>
        <Routes>
          <Route path="/" element={<ProductList />} />
          <Route path="/search" element={<SearchResults />} />
          <Route path="/product/:id" element={<ProductDetail />} />
          <Route path="/login" element={FEATURES.kakaoAuth ? <Login /> : <Navigate to="/" replace />} />
          <Route path="/mypage" element={FEATURES.kakaoAuth ? <MyPage /> : <Navigate to="/" replace />} />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </main>
      <footer className="site-footer">
        <p>
          문의:{' '}
          <a href="mailto:dealpopcontact@gmail.com">dealpopcontact@gmail.com</a>
        </p>
      </footer>
    </BrowserRouter>
  );
}

export default App;
