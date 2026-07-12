import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';

export default function Signup() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('TEAM_REP');
  const [teamId, setTeamId] = useState('');
  const [teams, setTeams] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    api.get('/teams').catch(() => null).then((res) => {
      // Teams endpoint requires auth; if it fails (not logged in yet),
      // silently fall back to manual team ID entry isn't ideal, so we try
      // a lightweight unauthenticated approach isn't available — this call
      // may 401 for a brand-new visitor, which is fine, just leave list empty.
      if (res) setTeams(res.data);
    });
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const payload = {
        username, email, password, role,
        teamId: role === 'TEAM_REP' ? Number(teamId) : null
      };
      const res = await register(payload);
      navigate(res.role === 'AUCTIONEER' ? '/admin' : '/room');
    } catch (err) {
      setError(err.response?.data?.error || 'Could not create account');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-shell">
      <div className="panel auth-card">
        <h1>Join In</h1>
        <div className="sub">Create your auction account.</div>

        {error && <div className="error-banner">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="field">
            <label>Username</label>
            <input value={username} onChange={(e) => setUsername(e.target.value)} required />
          </div>
          <div className="field">
            <label>Email</label>
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </div>
          <div className="field">
            <label>Password</label>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </div>
          <div className="field">
            <label>Role</label>
            <select value={role} onChange={(e) => setRole(e.target.value)}>
              <option value="TEAM_REP">Team Representative</option>
              <option value="AUCTIONEER">Auctioneer</option>
            </select>
          </div>
          {role === 'TEAM_REP' && (
            <div className="field">
              <label>Team</label>
              {teams.length > 0 ? (
                <select value={teamId} onChange={(e) => setTeamId(e.target.value)} required>
                  <option value="">Select a team…</option>
                  {teams.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
                </select>
              ) : (
                <input
                  placeholder="Team ID (log in as admin first to see team names)"
                  value={teamId}
                  onChange={(e) => setTeamId(e.target.value)}
                  required
                />
              )}
            </div>
          )}
          <button className="btn btn-gold btn-block" disabled={loading}>
            {loading ? 'Creating account…' : 'Create Account'}
          </button>
        </form>

        <div className="hint">
          Already have an account? <Link to="/login" style={{ color: 'var(--gold)' }}>Sign in</Link>
        </div>
      </div>
    </div>
  );
}
