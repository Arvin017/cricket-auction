import { formatRupees } from '../utils/format';

export default function SoldOverlay({ type, playerName, amount, teamName, onDone }) {
  if (type !== 'SOLD' && type !== 'UNSOLD') return null;

  return (
    <div className="sold-overlay" onAnimationEnd={onDone}>
      <div style={{ textAlign: 'center' }}>
        <div className={`sold-stamp ${type === 'UNSOLD' ? 'unsold' : ''}`}>
          {type === 'SOLD' ? 'SOLD' : 'UNSOLD'}
        </div>
        {type === 'SOLD' && (
          <div className="sold-price">{teamName} · {formatRupees(amount)}</div>
        )}
      </div>
    </div>
  );
}
