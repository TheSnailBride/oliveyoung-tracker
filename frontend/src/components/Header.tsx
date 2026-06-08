import { Link } from 'react-router-dom';
import { User } from 'lucide-react';
import { FEATURES } from '../config/features';

function Header() {
  return (
    <header className="header">
      <div className="header-inner">
        <Link to="/" className="logo">
          <div className="nav-brand" aria-label="Dealpop">
            <div className="nav-brand-word" aria-hidden="true">
              Dealpop
            </div>
          </div>
        </Link>
        {FEATURES.kakaoAuth && (
          <nav className="header-nav">
            <Link to="/mypage" className="nav-icon-link">
              <User size={20} />
            </Link>
          </nav>
        )}
      </div>
    </header>
  );
}

export default Header;
