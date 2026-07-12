import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await login(username, password);
      navigate(res.role === 'AUCTIONEER' ? '/admin' : '/room');
    } catch (err) {
      setError(err.response?.data?.error || 'Login failed — check your credentials');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-shell">
      <div className="panel auth-card">
        <h1>StrikeZone</h1>
        <div className="sub">Sign in to join the auction room.</div>

        {error && <div className="error-banner">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="field">
            <label>Username</label>
            <input value={username} onChange={(e) => setUsername(e.target.value)} autoFocus required />
          </div>
          <div className="field">
            <label>Password</label>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </div>
          <button className="btn btn-gold btn-block" disabled={loading}>
            {loading ? 'Signing in…' : 'Sign In'}
          </button>
        </form>

        <div className="hint">
          Demo logins — Auctioneer: <b>admin</b> / <b>admin123</b><br />
          Team rep: <b>mumbaimonarchs</b> / <b>team123</b> (or any seeded team)
        </div>

        <div className="hint">
          No account? <Link to="/signup" style={{ color: 'var(--gold)' }}>Create one</Link>
        </div>
      </div>
    </div>
  );
}
