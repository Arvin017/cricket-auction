package com.auction.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A persisted record of the auction's overall run. The LIVE, fast-changing
 * state (current player on the block, current highest bid, timer) lives in
 * Redis for speed and atomic updates — this table is just the durable
 * source of truth for status/history, updated whenever something important
 * happens (session started, player finalized, session ended).
 */
@Entity
@Table(name = "auction_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuctionSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AuctionStatus status = AuctionStatus.NOT_STARTED;

    // Player currently on the block, if any.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_player_id")
    private Player currentPlayer;

    @Builder.Default
    private Instant startedAt = null;

    @Builder.Default
    private Instant endedAt = null;
}
