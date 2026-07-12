import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function TopNav() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="top-nav">
      <Link to="/room" className="brand">
        StrikeZone<span className="dot">•</span>Auctions
      </Link>
      {user && (
        <div className="nav-links">
          <Link to="/room">Live Room</Link>
          {user.role === 'TEAM_REP' && <Link to={`/dashboard/${user.teamId}`}>My Team</Link>}
          {user.role === 'AUCTIONEER' && <Link to="/admin">Admin</Link>}
          <span className="nav-pill">{user.username} · {user.role === 'TEAM_REP' ? user.teamName : 'Auctioneer'}</span>
          <button className="linklike" onClick={handleLogout}>Log out</button>
        </div>
      )}
    </div>
  );
}
