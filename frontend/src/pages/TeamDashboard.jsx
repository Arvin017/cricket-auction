import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import api from '../api/axios';
import { formatRupees, roleLabel } from '../utils/format';

export default function TeamDashboard() {
  const { teamId } = useParams();
  const [dash, setDash] = useState(null);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    const res = await api.get(`/teams/${teamId}/dashboard`);
    setDash(res.data);
    setLoading(false);
  };

  useEffect(() => {
    load();
    const interval = setInterval(load, 8000); // light polling to stay fresh between socket events
    return () => clearInterval(interval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [teamId]);

  if (loading || !dash) {
    return <div className="panel">Loading team dashboard…</div>;
  }

  const { team, purseRemaining, purseTotal, squadSize, squadSizeMax, squadSizeMin, roleBreakdown, players } = dash;
  const spentPct = Math.round(((purseTotal - purseRemaining) / purseTotal) * 100);

  return (
    <div>
      <div className="dash-header">
        <img className="dash-logo" src={team.logoUrl} alt={team.name} />
        <div>
          <h1 className="dash-title">{team.name}</h1>
          <div style={{ color: 'var(--text-mid)', fontSize: 13 }}>
            Squad {squadSize}/{squadSizeMax} · minimum required {squadSizeMin}
          </div>
        </div>
      </div>

      <div className="stat-row">
        <div className="stat-card">
          <div className="num">{formatRupees(purseRemaining)}</div>
          <div className="lbl">Purse Remaining</div>
          <div className="purse-bar-track"><div className="purse-bar-fill" style={{ width: `${100 - spentPct}%` }} /></div>
        </div>
        <div className="stat-card">
          <div className="num">{formatRupees(purseTotal - purseRemaining)}</div>
          <div className="lbl">Spent So Far</div>
        </div>
        <div className="stat-card">
          <div className="num">{squadSize}</div>
          <div className="lbl">Players Acquired</div>
        </div>
      </div>

      <div className="section-title">Role Breakdown</div>
      <div className="role-breakdown">
        {['BATSMAN', 'BOWLER', 'ALL_ROUNDER', 'WICKETKEEPER'].map((role) => (
          <span className={`role-badge ${role}`} key={role}>
            {roleBreakdown[role] || 0} {roleLabel(role)}{(roleBreakdown[role] || 0) === 1 ? '' : 's'}
          </span>
        ))}
      </div>

      <div className="section-title">Squad</div>
      <div className="panel" style={{ padding: 0 }}>
        <table className="squad-table">
          <thead>
            <tr>
              <th>Player</th>
              <th>Role</th>
              <th>Nationality</th>
              <th>Price</th>
            </tr>
          </thead>
          <tbody>
            {players.length === 0 && (
              <tr><td colSpan={4} style={{ color: 'var(--text-low)' }}>No players acquired yet.</td></tr>
            )}
            {players.map((p) => (
              <tr key={p.id}>
                <td>{p.name}</td>
                <td><span className={`role-badge ${p.role}`}>{roleLabel(p.role)}</span></td>
                <td>{p.nationality}</td>
                <td className="price">{formatRupees(p.soldPrice)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
