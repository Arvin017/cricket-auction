package com.auction.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlayerRole role;

    private String nationality;

    // Base/reserve price in rupees.
    @Column(nullable = false)
    private Long basePrice;

    // Free-text optional stats, e.g. "412 runs, avg 34.2" or a JSON blob.
    @Column(length = 1000)
    private String stats;

    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PlayerStatus status = PlayerStatus.UPCOMING;

    // Final price the player sold for, if sold.
    private Long soldPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sold_to_team_id")
    private Team soldToTeam;
}
