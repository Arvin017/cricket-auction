/**
 * Formats a rupee amount (integer, e.g. 20000000) into IPL-style
 * "₹2 Cr" / "₹35 L" shorthand.
 */
export function formatRupees(amount) {
  if (amount === null || amount === undefined) return '—';
  const n = Number(amount);
  if (n >= 10000000) {
    const cr = n / 10000000;
    return `₹${trimZero(cr)} Cr`;
  }
  if (n >= 100000) {
    const l = n / 100000;
    return `₹${trimZero(l)} L`;
  }
  return `₹${n.toLocaleString('en-IN')}`;
}

function trimZero(n) {
  return Number.isInteger(n) ? n.toString() : n.toFixed(2).replace(/0+$/, '').replace(/\.$/, '');
}

export function roleLabel(role) {
  switch (role) {
    case 'BATSMAN': return 'Batsman';
    case 'BOWLER': return 'Bowler';
    case 'ALL_ROUNDER': return 'All-rounder';
    case 'WICKETKEEPER': return 'Wicketkeeper';
    default: return role;
  }
}
