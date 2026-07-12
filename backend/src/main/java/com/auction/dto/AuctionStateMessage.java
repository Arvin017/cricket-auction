package com.auction.dto;

/**
 * Broadcast over /topic/auction whenever the live auction state changes:
 * a new player goes on the block, a bid is accepted, the timer resets,
 * or a player is finalized as SOLD/UNSOLD.
 */
public record AuctionStateMessage(
        String type,          // PLAYER_ON_BLOCK | BID_ACCEPTED | BID_REJECTED | SOLD | UNSOLD | PAUSED | RESUMED | ENDED
        Long playerId,
        String playerName,
        Long currentAmount,
        Long currentTeamId,
        String currentTeamName,
        Integer timerSeconds,
        String message
) {}
