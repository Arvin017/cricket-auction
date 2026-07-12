import { useEffect, useState } from 'react';

const RADIUS = 32;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;

/**
 * Renders a circular countdown that runs locally in the browser, resetting
 * every time `resetToken` changes (i.e. every time the server broadcasts a
 * new timer value after a bid or a new player going on the block).
 */
export default function CountdownRing({ totalSeconds, resetToken }) {
  const [secondsLeft, setSecondsLeft] = useState(totalSeconds);

  useEffect(() => {
    setSecondsLeft(totalSeconds);
  }, [resetToken, totalSeconds]);

  useEffect(() => {
    if (secondsLeft <= 0) return;
    const t = setTimeout(() => setSecondsLeft((s) => Math.max(0, s - 1)), 1000);
    return () => clearTimeout(t);
  }, [secondsLeft]);

  const pct = totalSeconds > 0 ? secondsLeft / totalSeconds : 0;
  const offset = CIRCUMFERENCE * (1 - pct);
  const urgent = secondsLeft <= 5;

  return (
    <div className={`timer-ring ${urgent ? 'urgent' : ''}`}>
      <svg width="76" height="76" viewBox="0 0 76 76">
        <circle className="track" cx="38" cy="38" r={RADIUS} />
        <circle
          className="progress"
          cx="38" cy="38" r={RADIUS}
          strokeDasharray={CIRCUMFERENCE}
          strokeDashoffset={offset}
        />
      </svg>
      <div className="value">{secondsLeft}</div>
    </div>
  );
}
