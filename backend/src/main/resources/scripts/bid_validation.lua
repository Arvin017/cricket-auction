-- bid_validation.lua
--
-- Atomically validates and applies a bid for the player currently on the
-- auction block. This is the core technical piece of the project: without
-- it, two team reps clicking "bid" within milliseconds of each other could
-- both read the same "current highest bid", both think their bid is valid,
-- and both get accepted by the application server — a classic race
-- condition. Because this whole check-and-set happens as a single Lua
-- script inside Redis, Redis executes it as one atomic, uninterruptible
-- operation: only one of the two competing bids can ever win.
--
-- KEYS[1] = the Redis hash key holding current auction state,
--           e.g. "auction:current"
--           Hash fields: playerId, amount, teamId, basePrice
--
-- ARGV[1] = playerId the bid is FOR (guards against a stale/late bid for a
--           player who has already been sold and replaced on the block)
-- ARGV[2] = proposed bid amount (integer rupees)
-- ARGV[3] = bidding team's id
-- ARGV[4] = the player's base price (only used to seed the very first bid)
--
-- Returns a 3-element array: { status, currentAmount, currentTeamId }
--   status = 1  -> bid ACCEPTED, hash has been updated
--   status = 0  -> bid REJECTED (not a valid increment / not higher)
--   status = -1 -> bid REJECTED (wrong/stale player id — the block has moved on)

local auctionKey = KEYS[1]

local bidPlayerId = ARGV[1]
local bidAmount = tonumber(ARGV[2])
local bidTeamId = ARGV[3]
local basePrice = tonumber(ARGV[4])

local currentPlayerId = redis.call('HGET', auctionKey, 'playerId')
local currentAmountRaw = redis.call('HGET', auctionKey, 'amount')
local currentTeamId = redis.call('HGET', auctionKey, 'teamId')

-- Guard: reject any bid unless it is for the EXACT player currently on the
-- block. Two cases:
--   1. currentPlayerId is false -> no player is on the block at all right
--      now (between rounds, or auction not started yet).
--   2. currentPlayerId is set but differs from bidPlayerId -> the block has
--      already moved on to a different player (a stale/late network bid).
-- Without this combined check, a stale bid arriving in the gap right after
-- a sale is finalized (when the hash has just been deleted) could be
-- misread as a legitimate "first bid" on a fresh auction.
if currentPlayerId == false then
    return { -1, 0, '' }
end
if currentPlayerId ~= bidPlayerId then
    return { -1, tonumber(currentAmountRaw) or 0, currentTeamId or '' }
end

local currentAmount
local isFirstBid = (currentAmountRaw == false)

if isFirstBid then
    currentAmount = basePrice
else
    currentAmount = tonumber(currentAmountRaw)
end

-- Determine the required increment based on the CURRENT amount's slab.
-- Keep these thresholds in sync with application.yml.
local increment
if currentAmount < 10000000 then
    -- below 1 Cr -> 10 Lakh steps
    increment = 1000000
elseif currentAmount < 20000000 then
    -- 1 Cr to 2 Cr -> 20 Lakh steps
    increment = 2000000
else
    -- above 2 Cr -> 25 Lakh steps
    increment = 2500000
end

local expectedAmount
if isFirstBid then
    -- The opening bid on a fresh player must equal the base price exactly.
    expectedAmount = basePrice
else
    expectedAmount = currentAmount + increment
end

if bidAmount ~= expectedAmount then
    -- Not the exact next valid increment (either too low, too high, or a
    -- stale amount computed against an outdated "current" value).
    return { 0, currentAmount, currentTeamId or '' }
end

-- Bid is valid: atomically write the new state.
redis.call('HSET', auctionKey,
    'playerId', bidPlayerId,
    'amount', tostring(bidAmount),
    'teamId', bidTeamId)

return { 1, bidAmount, bidTeamId }
