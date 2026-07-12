package com.auction.service;

import com.auction.dto.TeamRequest;
import com.auction.entity.Player;
import com.auction.entity.PlayerRole;
import com.auction.entity.Team;
import com.auction.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;

    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    public Team getTeam(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + id));
    }

    public Team createTeam(TeamRequest request) {
        Team team = Team.builder()
                .name(request.name())
                .logoUrl(request.logoUrl())
                .purseTotal(request.purseTotal())
                .purseRemaining(request.purseTotal())
                .squadSizeMax(request.squadSizeMax() != null ? request.squadSizeMax() : 25)
                .squadSizeMin(request.squadSizeMin() != null ? request.squadSizeMin() : 18)
                .build();
        return teamRepository.save(team);
    }

    /**
     * Squad breakdown for a team's dashboard: role -> count of players won.
     */
    public Map<PlayerRole, Long> squadBreakdown(Team team) {
        List<Player> players = team.getPlayers();
        return players.stream()
                .collect(Collectors.groupingBy(Player::getRole, Collectors.counting()));
    }

    /**
     * The maximum a team can legally bid right now, after reserving a
     * base-price slot (10 Lakh floor, the lowest realistic base price) for
     * every remaining MANDATORY squad slot it still needs to fill. Mirrors
     * real IPL purse-management rules so a team can't spend itself into a
     * position where it can't complete its minimum squad.
     */
    public long maxAllowableBid(Team team, long minimumReservePerSlot) {
        long currentSquadSize = team.getPlayers().size();
        long remainingMandatorySlots = team.remainingSlotsToFill(currentSquadSize);
        // We're about to potentially win the CURRENT player too, so reserve
        // for slots still needed AFTER this one (hence -1, floored at 0).
        long slotsToReserveFor = Math.max(0, remainingMandatorySlots - 1);
        long reserve = slotsToReserveFor * minimumReservePerSlot;
        return Math.max(0, team.getPurseRemaining() - reserve);
    }
}
