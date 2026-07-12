/**
 * Mirrors backend/src/main/resources/scripts/bid_validation.lua exactly.
 * Used only to SHOW the next valid bid amount as a button label — the
 * server is the actual source of truth and re-validates atomically.
 */
export function nextBidAmount(currentAmount, basePrice) {
  if (currentAmount === null || currentAmount === undefined || currentAmount === 0) {
    return basePrice;
  }
  let increment;
  if (currentAmount < 10000000) {
    increment = 1000000;
  } else if (currentAmount < 20000000) {
    increment = 2000000;
  } else {
    increment = 2500000;
  }
  return currentAmount + increment;
}
