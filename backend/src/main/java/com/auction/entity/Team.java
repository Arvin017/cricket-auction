package com.auction.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "teams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String logoUrl;

    // Total purse the team started the auction with, in rupees.
    @Column(nullable = false)
    private Long purseTotal;

    // Purse remaining right now, in rupees. Decreases as players are won.
    @Column(nullable = false)
    private Long purseRemaining;

    @Column(nullable = false)
    @Builder.Default
    private Integer squadSizeMax = 25;

    // Minimum number of players a team MUST end up with; used to reserve
    // enough purse so the team can't spend itself into a corner.
    @Column(nullable = false)
    @Builder.Default
    private Integer squadSizeMin = 18;

    @OneToMany(mappedBy = "soldToTeam", fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private List<Player> players = new ArrayList<>();

    /**
     * Purse a team is allowed to actually commit to the CURRENT bid, after
     * reserving a base-price slot for every remaining mandatory squad spot.
     * This mirrors real IPL rules: you can't blow your whole purse on one
     * player if you still need to fill your minimum squad.
     */
    @Transient
    public long remainingSlotsToFill(long currentSquadSize) {
        return Math.max(0, squadSizeMin - currentSquadSize);
    }
}
