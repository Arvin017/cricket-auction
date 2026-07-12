import { useEffect, useMemo, useState } from 'react';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';
import { useAuctionSocket } from '../hooks/useAuctionSocket';
import CountdownRing from '../components/CountdownRing';
import SoldOverlay from '../components/SoldOverlay';
import { formatRupees, roleLabel } from '../utils/format';
import { nextBidAmount } from '../utils/bidLogic';

export default function AuctionRoom() {
  const { user } = useAuth();
  const { connected, lastMessage, feed } = useAuctionSocket();

  const [currentPlayer, setCurrentPlayer] = useState(null); // full Player object
  const [currentAmount, setCurrentAmount] = useState(0);
  const [currentTeamName, setCurrentTeamName] = useState(null);
  const [timerSeconds, setTimerSeconds] = useState(30);
  const [timerToken, setTimerToken] = useState(0);
  const [paused, setPaused] = useState(false);
  const [soldEvent, setSoldEvent] = useState(null);
  const [bidError, setBidError] = useState('');
  const [upcoming, setUpcoming] = useState([]);
  const [myTeam, setMyTeam] = useState(null);
  const [placing, setPlacing] = useState(false);

  const loadUpcoming = async () => {
    const res = await api.get('/players/upcoming');
    setUpcoming(res.data);
  };

  const loadMyTeam = async () => {
    if (user?.role === 'TEAM_REP' && user.teamId) {
      const res = await api.get(`/teams/${user.teamId}/dashboard`);
      setMyTeam(res.data);
    }
  };

  useEffect(() => {
    loadUpcoming();
    loadMyTeam();
    api.get('/auction/state').then(async (res) => {
      const state = res.data;
      if (state && state.playerId) {
        const playerRes = await api.get(`/players/${state.playerId}`);
        setCurrentPlayer(playerRes.data);
        setCurrentAmount(state.amount ? Number(state.amount) : 0);
        if (state.teamId) {
          const teamRes = await api.get(`/teams/${state.teamId}`);
          setCurrentTeamName(teamRes.data.name);
        }
        setTimerSeconds(30);
        setTimerToken((t) => t + 1);
      }
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!lastMessage) return;
    const msg = lastMessage;
    setBidError('');

    switch (msg.type) {
      case 'PLAYER_ON_BLOCK': {
        setSoldEvent(null);
        api.get(`/players/${msg.playerId}`).then((res) => setCurrentPlayer(res.data));
        setCurrentAmount(0);
        setCurrentTeamName(null);
        setTimerSeconds(msg.timerSeconds || 30);
        setTimerToken((t) => t + 1);
        setPaused(false);
        loadUpcoming();
        break;
      }
      case 'BID_ACCEPTED': {
        setCurrentAmount(msg.currentAmount);
        setCurrentTeamName(msg.currentTeamName);
        setTimerSeconds(msg.timerSeconds || 30);
        setTimerToken((t) => t + 1);
        break;
      }
      case 'SOLD':
        setSoldEvent({ type: 'SOLD', playerName: msg.playerName, amount: msg.currentAmount, teamName: msg.currentTeamName });
        loadMyTeam();
        loadUpcoming();
        break;
      case 'UNSOLD':
        setSoldEvent({ type: 'UNSOLD', playerName: msg.playerName });
        loadUpcoming();
        break;
      case 'PAUSED':
        setPaused(true);
        break;
      case 'RESUMED':
        setPaused(false);
        setTimerSeconds(msg.timerSeconds || 30);
        setTimerToken((t) => t + 1);
        break;
      default:
        break;
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [lastMessage]);

  const handleSoldDone = () => {
    setSoldEvent(null);
    setCurrentPlayer(null);
    setCurrentAmount(0);
    setCurrentTeamName(null);
  };

  const proposedAmount = useMemo(() => {
    if (!currentPlayer) return null;
    return nextBidAmount(currentAmount, currentPlayer.basePrice);
  }, [currentAmount, currentPlayer]);

  const placeBid = async () => {
    if (!currentPlayer || !proposedAmount) return;
    setPlacing(true);
    setBidError('');
    try {
      const res = await api.post('/auction/bid', {
        playerId: currentPlayer.id,
        amount: proposedAmount
      });
      if (res.data.type === 'BID_REJECTED') {
        setBidError(res.data.message);
      }
    } catch (e) {
      setBidError(e.response?.data?.error || 'Bid failed — try again');
    } finally {
      setPlacing(false);
    }
  };

  const recentBids = feed.filter((m) => m.type === 'BID_ACCEPTED').slice(-12).reverse();

  return (
    <div>
      {!connected && (
        <div className="error-banner" style={{ marginBottom: 16 }}>
          Connecting to the live auction feed…
        </div>
      )}

      <div className="next-up-strip">
        <span className="label">Next up</span>
        {upcoming.slice(0, 6).map((p) => (
          <span className="chip" key={p.id}>{p.name} · {formatRupees(p.basePrice)}</span>
        ))}
        {upcoming.length === 0 && <span className="chip">Pool is empty — waiting for the auctioneer</span>}
      </div>

      <div className="room-grid">
        <div className={`block-stage ${paused ? 'paused' : ''}`}>
          {soldEvent && <SoldOverlay {...soldEvent} onDone={handleSoldDone} />}

          {currentPlayer ? (
            <>
              <div className="stage-top">
                <div className="player-identity">
                  <img className="player-photo" src={currentPlayer.photoUrl} alt={currentPlayer.name} />
                  <div>
                    <h1 className="player-name">{currentPlayer.name}</h1>
                    <div className="player-meta">
                      <span className={`role-badge ${currentPlayer.role}`}>{roleLabel(currentPlayer.role)}</span>
                      <span>{currentPlayer.nationality}</span>
                      <span>· Base {formatRupees(currentPlayer.basePrice)}</span>
                    </div>
                    {currentPlayer.stats && <div className="player-stats">{currentPlayer.stats}</div>}
                  </div>
                </div>
                <CountdownRing totalSeconds={timerSeconds} resetToken={timerToken} />
              </div>

              <div className="bid-showcase">
                <div className="bid-label">Current Bid</div>
                <div className="bid-amount pulse" key={currentAmount}>{formatRupees(currentAmount || currentPlayer.basePrice)}</div>
                {currentTeamName ? (
                  <div className="bid-holder">held by <b>{currentTeamName}</b></div>
                ) : (
                  <div className="no-bid-yet">No bids yet — base price shown</div>
                )}
              </div>

              {user?.role === 'TEAM_REP' && (
                <div>
                  <div className="bid-controls">
                    <button className="btn btn-gold" onClick={placeBid} disabled={placing || paused}>
                      Bid {formatRupees(proposedAmount)}
                    </button>
                  </div>
                  <div className="bid-error">{bidError}</div>
                </div>
              )}
            </>
          ) : (
            <div style={{ textAlign: 'center', padding: '80px 0', color: 'var(--text-low)' }}>
              <div style={{ fontFamily: 'var(--font-display)', fontSize: 30, color: 'var(--text-mid)', marginBottom: 8 }}>
                Waiting for the next player
              </div>
              The auctioneer hasn't started bidding yet.
            </div>
          )}
        </div>

        <div className="side-rail">
          {myTeam && (
            <div className="rail-panel myteam-card">
              <div className="rail-title">My Team · {myTeam.team.name}</div>
              <div className="purse">{formatRupees(myTeam.purseRemaining)}</div>
              <div className="purse-sub">remaining of {formatRupees(myTeam.purseTotal)} · {myTeam.squadSize}/{myTeam.squadSizeMax} squad</div>
            </div>
          )}

          <div className="rail-panel">
            <div className="rail-title">Bid History</div>
            <div className="bid-feed">
              {recentBids.length === 0 && <div className="empty-feed">No bids yet this round.</div>}
              {recentBids.map((b, i) => (
                <div className="bid-feed-item" key={i}>
                  <span className="team">{b.currentTeamName}</span>
                  <span className="amount">{formatRupees(b.currentAmount)}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
