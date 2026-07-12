import { useEffect, useState } from 'react';
import api from '../api/axios';
import { useAuctionSocket } from '../hooks/useAuctionSocket';
import { formatRupees, roleLabel } from '../utils/format';

const emptyForm = { name: '', role: 'BATSMAN', nationality: '', basePrice: '', stats: '', photoUrl: '' };

export default function AdminPanel() {
  const { lastMessage } = useAuctionSocket();
  const [players, setPlayers] = useState([]);
  const [activePlayerId, setActivePlayerId] = useState(null);
  const [busy, setBusy] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [formError, setFormError] = useState('');
  const [actionError, setActionError] = useState('');
  const [tab, setTab] = useState('pool');

  const loadPlayers = async () => {
    const res = await api.get('/players');
    setPlayers(res.data);
  };

  useEffect(() => {
    loadPlayers();
    api.get('/auction/state').then((res) => {
      if (res.data && res.data.playerId) setActivePlayerId(Number(res.data.playerId));
    });
  }, []);

  useEffect(() => {
    if (!lastMessage) return;
    if (lastMessage.type === 'PLAYER_ON_BLOCK') {
      setActivePlayerId(lastMessage.playerId);
    }
    if (lastMessage.type === 'SOLD' || lastMessage.type === 'UNSOLD') {
      setActivePlayerId(null);
      loadPlayers();
    }
  }, [lastMessage]);

  const startPlayer = async (id) => {
    setBusy(true); setActionError('');
    try {
      await api.post(`/admin/auction/start/${id}`);
    } catch (e) {
      setActionError(e.response?.data?.error || 'Could not start this player');
    } finally {
      setBusy(false);
    }
  };

  const doAction = async (path) => {
    setBusy(true); setActionError('');
    try {
      await api.post(`/admin/auction/${path}`);
    } catch (e) {
      setActionError(e.response?.data?.error || 'Action failed');
    } finally {
      setBusy(false);
    }
  };

  const submitPlayer = async (e) => {
    e.preventDefault();
    setFormError('');
    if (!form.name || !form.basePrice) {
      setFormError('Name and base price are required');
      return;
    }
    try {
      await api.post('/players', {
        ...form,
        basePrice: Number(form.basePrice),
        photoUrl: form.photoUrl || `https://api.dicebear.com/7.x/personas/svg?seed=${encodeURIComponent(form.name)}`
      });
      setForm(emptyForm);
      loadPlayers();
      setTab('pool');
    } catch (e) {
      setFormError(e.response?.data?.error || 'Could not add player');
    }
  };

  const upcoming = players.filter((p) => p.status === 'UPCOMING' || p.status === 'ON_BLOCK');
  const done = players.filter((p) => p.status === 'SOLD' || p.status === 'UNSOLD');

  return (
    <div>
      <h1 className="dash-title" style={{ marginBottom: 18 }}>Auctioneer Console</h1>

      <div className="control-bar">
        <button className="btn btn-outline" onClick={() => doAction('pause')} disabled={busy || !activePlayerId}>Pause</button>
        <button className="btn btn-outline" onClick={() => doAction('resume')} disabled={busy || !activePlayerId}>Resume</button>
        <button className="btn btn-green" onClick={() => doAction('finalize')} disabled={busy || !activePlayerId}>
          Finalize Sale (SOLD / UNSOLD)
        </button>
      </div>
      {actionError && <div className="error-banner">{actionError}</div>}

      <div className="admin-layout">
        <div className="panel">
          <div className="tabs">
            <button className={`tab-btn ${tab === 'pool' ? 'active' : ''}`} onClick={() => setTab('pool')}>Player Pool</button>
            <button className={`tab-btn ${tab === 'add' ? 'active' : ''}`} onClick={() => setTab('add')}>Add Player</button>
          </div>

          {tab === 'pool' && (
            <div className="player-pool-list">
              {upcoming.length === 0 && <div style={{ color: 'var(--text-low)', fontSize: 13 }}>No upcoming players.</div>}
              {upcoming.map((p) => (
                <div className={`pool-row ${p.id === activePlayerId ? 'active-block' : ''}`} key={p.id}>
                  <div className="who">
                    <img src={p.photoUrl} alt={p.name} />
                    <div>
                      <div className="name">{p.name}</div>
                      <div className="base">{roleLabel(p.role)} · {formatRupees(p.basePrice)}</div>
                    </div>
                  </div>
                  {p.id === activePlayerId ? (
                    <span className="status-pill ON_BLOCK">On the block</span>
                  ) : (
                    <button className="btn btn-gold btn-sm" onClick={() => startPlayer(p.id)} disabled={busy || !!activePlayerId}>
                      Start
                    </button>
                  )}
                </div>
              ))}

              {done.length > 0 && (
                <>
                  <div className="rail-title" style={{ marginTop: 14 }}>Completed</div>
                  {done.map((p) => (
                    <div className="pool-row" key={p.id}>
                      <div className="who">
                        <img src={p.photoUrl} alt={p.name} />
                        <div>
                          <div className="name">{p.name}</div>
                          <div className="base">{p.status === 'SOLD' ? formatRupees(p.soldPrice) : 'Went unsold'}</div>
                        </div>
                      </div>
                      <span className={`status-pill ${p.status}`}>{p.status}</span>
                    </div>
                  ))}
                </>
              )}
            </div>
          )}

          {tab === 'add' && (
            <form onSubmit={submitPlayer}>
              {formError && <div className="error-banner">{formError}</div>}
              <div className="add-player-form">
                <div className="field full">
                  <label>Name</label>
                  <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
                </div>
                <div className="field">
                  <label>Role</label>
                  <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
                    <option value="BATSMAN">Batsman</option>
                    <option value="BOWLER">Bowler</option>
                    <option value="ALL_ROUNDER">All-rounder</option>
                    <option value="WICKETKEEPER">Wicketkeeper</option>
                  </select>
                </div>
                <div className="field">
                  <label>Nationality</label>
                  <input value={form.nationality} onChange={(e) => setForm({ ...form, nationality: e.target.value })} />
                </div>
                <div className="field">
                  <label>Base Price (₹)</label>
                  <input type="number" value={form.basePrice} onChange={(e) => setForm({ ...form, basePrice: e.target.value })} />
                </div>
                <div className="field">
                  <label>Photo URL (optional)</label>
                  <input value={form.photoUrl} onChange={(e) => setForm({ ...form, photoUrl: e.target.value })} />
                </div>
                <div className="field full">
                  <label>Stats (optional)</label>
                  <input value={form.stats} onChange={(e) => setForm({ ...form, stats: e.target.value })} />
                </div>
              </div>
              <button className="btn btn-gold">Add to Pool</button>
            </form>
          )}
        </div>

        <div className="panel">
          <div className="rail-title">Live Room Preview</div>
          <p style={{ color: 'var(--text-mid)', fontSize: 13.5 }}>
            Open the <a href="/room" style={{ color: 'var(--gold)' }}>Live Room</a> in another tab (or have team reps
            log in there) to watch bidding happen in real time as you control the flow from here.
          </p>
        </div>
      </div>
    </div>
  );
}
