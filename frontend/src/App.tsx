import { useEffect } from 'react';
import { BrowserRouter, Routes, Route, useLocation, useNavigate } from 'react-router-dom';
import ProductList from './pages/ProductList';
import ProductDetail from './pages/ProductDetail';
import Login from './pages/Login';
import Alerts from './pages/Alerts';
import MyPage from './pages/MyPage';
import Header from './components/Header';

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
      <TokenHandler />
      <Header />
      <main style={{ paddingBottom: '80px' }}>
        <Routes>
          <Route path="/" element={<ProductList />} />
          <Route path="/product/:id" element={<ProductDetail />} />
          <Route path="/login" element={<Login />} />
          <Route path="/alerts" element={<Alerts />} />
          <Route path="/mypage" element={<MyPage />} />
        </Routes>
      </main>
    </BrowserRouter>
  );
}

export default App;
