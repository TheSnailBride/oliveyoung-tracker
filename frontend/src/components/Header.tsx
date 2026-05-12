import { Link } from 'react-router-dom';
import { Bell, User } from 'lucide-react';

function Header() {
  return (
    <header className="header">
      <div className="header-inner">
        <Link to="/" className="logo">
          <div className="logo-text">Olive<span>Tracker</span></div>
        </Link>
        <nav className="header-nav">
          <Link to="/alerts" className="nav-icon-link">
            <Bell size={20} />
          </Link>
          <Link to="/mypage" className="nav-icon-link">
            <User size={20} />
          </Link>
        </nav>
      </div>
    </header>
  );
}

export default Header;
