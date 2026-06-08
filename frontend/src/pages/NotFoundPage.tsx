import { Link } from 'react-router-dom';

function NotFoundPage() {
  return (
    <section className="page-state">
      <p className="page-state-kicker">404</p>
      <h1>페이지를 찾을 수 없습니다.</h1>
      <p>주소가 잘못되었거나, 더 이상 사용할 수 없는 페이지입니다.</p>
      <Link to="/" className="page-state-primary">
        홈으로 돌아가기
      </Link>
    </section>
  );
}

export default NotFoundPage;
