package com.auction.service;

import com.auction.dto.AuctionStateMessage;
import com.auction.entity.*;
import com.auction.repository.AuctionSessionRepository;
import com.auction.repository.BidRepository;
import com.auction.repository.PlayerRepository;
import com.auction.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionService {

    private static final String AUCTION_KEY = "auction:current";
    private static final String TIMER_KEY = "auction:current:timerEndsAt";
    private static final long MIN_RESERVE_PER_SLOT = 1_000_000L; // 10 Lakh floor reserve per open squad slot

    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<List> bidValidationScript;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final BidRepository bidRepository;
    private final AuctionSessionRepository auctionSessionRepository;
    private final TeamService teamService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${app.auction.default-bid-timer-seconds:30}")
    private int defaultTimerSeconds;

    private volatile AuctionStatus currentStatus = AuctionStatus.NOT_STARTED;

    // ---------------------------------------------------------------
    // Auctioneer controls
    // ---------------------------------------------------------------

    @Transactional
    public synchronized AuctionStateMessage startPlayer(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));

        if (player.getStatus() == PlayerStatus.SOLD) {
            throw new IllegalStateException("Player already sold");
        }

        player.setStatus(PlayerStatus.ON_BLOCK);
        playerRepository.save(player);

        redisTemplate.delete(AUCTION_KEY);
        Map<String, String> initial = Map.of(
                "playerId", playerId.toString(),
                "basePrice", player.getBasePrice().toString()
        );
        redisTemplate.opsForHash().putAll(AUCTION_KEY, initial);

        resetTimer();
        currentStatus = AuctionStatus.IN_PROGRESS;

        AuctionStateMessage msg = new AuctionStateMessage(
                "PLAYER_ON_BLOCK",
                player.getId(),
                player.getName(),
                0L,
                null,
                null,
                defaultTimerSeconds,
                "Bidding open for " + player.getName() + " (base ₹" + player.getBasePrice() + ")"
        );
        broadcast(msg);
        return msg;
    }

    public synchronized AuctionStateMessage pause() {
        currentStatus = AuctionStatus.PAUSED;
        AuctionStateMessage msg = new AuctionStateMessage("PAUSED", null, null, null, null, null, null, "Auction paused");
        broadcast(msg);
        return msg;
    }

    public synchronized AuctionStateMessage resume() {
        currentStatus = AuctionStatus.IN_PROGRESS;
        resetTimer();
        AuctionStateMessage msg = new AuctionStateMessage("RESUMED", null, null, null, null, null, defaultTimerSeconds, "Auction resumed");
        broadcast(msg);
        return msg;
    }

    /**
     * Auctioneer manually finalizes the current player as SOLD (to whoever
     * currently holds the highest bid) or UNSOLD (if nobody has bid).
     */
    @Transactional
    public synchronized AuctionStateMessage finalizeCurrentPlayer() {
        Map<Object, Object> state = redisTemplate.opsForHash().entries(AUCTION_KEY);
        if (state.isEmpty() || state.get("playerId") == null) {
            throw new IllegalStateException("No player currently on the block");
        }

        Long playerId = Long.valueOf(state.get("playerId").toString());
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));

        Object teamIdObj = state.get("teamId");
        AuctionStateMessage msg;

        if (teamIdObj == null) {
            // Nobody bid.
            player.setStatus(PlayerStatus.UNSOLD);
            playerRepository.save(player);
            msg = new AuctionStateMessage("UNSOLD", player.getId(), player.getName(), null, null, null, null,
                    player.getName() + " went UNSOLD");
        } else {
            Long teamId = Long.valueOf(teamIdObj.toString());
            Long amount = Long.valueOf(state.get("amount").toString());
            Team team = teamRepository.findById(teamId)
                    .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

            player.setStatus(PlayerStatus.SOLD);
            player.setSoldPrice(amount);
            player.setSoldToTeam(team);
            playerRepository.save(player);

            team.setPurseRemaining(team.getPurseRemaining() - amount);
            teamRepository.save(team);

            msg = new AuctionStateMessage("SOLD", player.getId(), player.getName(), amount, teamId, team.getName(), null,
                    "SOLD! " + player.getName() + " to " + team.getName() + " for ₹" + amount);
        }

        redisTemplate.delete(AUCTION_KEY);
        redisTemplate.delete(TIMER_KEY);
        broadcast(msg);
        return msg;
    }

    // ---------------------------------------------------------------
    // Bidding (team reps)
    // ---------------------------------------------------------------

    /**
     * Places a bid using the atomic Redis Lua script. Returns the resulting
     * state message (accepted or rejected) which is ALSO broadcast to the
     * room so every connected client stays in sync.
     */
    @Transactional
    public AuctionStateMessage placeBid(Long playerId, Long teamId, Long amount) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));

        // Purse ceiling check happens application-side, BEFORE we touch
        // Redis: a team's own purse only changes when it wins a player, so
        // this doesn't need to be part of the atomic cross-team race check.
        long maxAllowable = teamService.maxAllowableBid(team, MIN_RESERVE_PER_SLOT);
        if (amount > maxAllowable) {
            return rejection(playerId, player.getName(),
                    "Bid exceeds " + team.getName() + "'s available purse (reserving for minimum squad size)");
        }
        if (team.getPlayers().size() >= team.getSquadSizeMax()) {
            return rejection(playerId, player.getName(), team.getName() + " squad is already full");
        }

        @SuppressWarnings("unchecked")
        List<Object> result = redisTemplate.execute(
                bidValidationScript,
                List.of(AUCTION_KEY),
                playerId.toString(),
                amount.toString(),
                teamId.toString(),
                player.getBasePrice().toString()
        );

        long status = ((Number) result.get(0)).longValue();
        long currentAmount = ((Number) result.get(1)).longValue();
        String currentTeamId = result.get(2) == null ? "" : result.get(2).toString();

        if (status == 1) {
            // Accepted: persist bid history and reset the countdown timer.
            Bid bid = Bid.builder()
                    .player(player)
                    .team(team)
                    .amount(amount)
                    .timestamp(Instant.now())
                    .build();
            bidRepository.save(bid);
            resetTimer();

            AuctionStateMessage msg = new AuctionStateMessage(
                    "BID_ACCEPTED", playerId, player.getName(), amount, teamId, team.getName(),
                    defaultTimerSeconds, team.getName() + " bids ₹" + amount);
            broadcast(msg);
            return msg;
        } else if (status == -1) {
            return rejection(playerId, player.getName(), "That player is no longer on the block");
        } else {
            return rejection(playerId, player.getName(),
                    "Bid rejected — current highest is ₹" + currentAmount +
                            (currentTeamId.isEmpty() ? "" : " (held by team #" + currentTeamId + ")") +
                            ". Bid the next valid increment.");
        }
    }

    private AuctionStateMessage rejection(Long playerId, String playerName, String reason) {
        // Rejections are NOT broadcast room-wide (only the bidder needs to
        // know); the caller (controller) returns this directly to them.
        return new AuctionStateMessage("BID_REJECTED", playerId, playerName, null, null, null, null, reason);
    }

    // ---------------------------------------------------------------
    // Timer (auto-finalize when countdown expires with no new bids)
    // ---------------------------------------------------------------

    private void resetTimer() {
        long endsAt = Instant.now().toEpochMilli() + (defaultTimerSeconds * 1000L);
        redisTemplate.opsForValue().set(TIMER_KEY, String.valueOf(endsAt), defaultTimerSeconds + 10, TimeUnit.SECONDS);
    }

    @Scheduled(fixedDelay = 1000)
    public void checkTimerExpiry() {
        if (currentStatus != AuctionStatus.IN_PROGRESS) {
            return;
        }
        String endsAtStr = redisTemplate.opsForValue().get(TIMER_KEY);
        if (endsAtStr == null) {
            return;
        }
        long endsAt = Long.parseLong(endsAtStr);
        if (Instant.now().toEpochMilli() >= endsAt) {
            Boolean hasPlayer = redisTemplate.hasKey(AUCTION_KEY);
            if (Boolean.TRUE.equals(hasPlayer)) {
                try {
                    finalizeCurrentPlayer();
                } catch (Exception e) {
                    log.warn("Auto-finalize on timer expiry failed: {}", e.getMessage());
                }
            }
        }
    }

    private void broadcast(AuctionStateMessage msg) {
        messagingTemplate.convertAndSend("/topic/auction", msg);
    }

    public Map<Object, Object> getCurrentState() {
        return redisTemplate.opsForHash().entries(AUCTION_KEY);
    }
}
