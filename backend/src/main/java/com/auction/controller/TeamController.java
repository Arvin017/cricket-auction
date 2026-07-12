package com.auction.controller;

import com.auction.dto.TeamRequest;
import com.auction.entity.PlayerRole;
import com.auction.entity.Team;
import com.auction.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    public ResponseEntity<List<Team>> getAllTeams() {
        return ResponseEntity.ok(teamService.getAllTeams());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Team> getTeam(@PathVariable Long id) {
        return ResponseEntity.ok(teamService.getTeam(id));
    }

    @GetMapping("/{id}/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard(@PathVariable Long id) {
        Team team = teamService.getTeam(id);
        Map<PlayerRole, Long> breakdown = teamService.squadBreakdown(team);
        Map<String, Object> dashboard = Map.of(
                "team", team,
                "purseRemaining", team.getPurseRemaining(),
                "purseTotal", team.getPurseTotal(),
                "squadSize", team.getPlayers().size(),
                "squadSizeMax", team.getSquadSizeMax(),
                "squadSizeMin", team.getSquadSizeMin(),
                "roleBreakdown", breakdown,
                "players", team.getPlayers()
        );
        return ResponseEntity.ok(dashboard);
    }

    @PostMapping
    @PreAuthorize("hasRole('AUCTIONEER')")
    public ResponseEntity<Team> createTeam(@Valid @RequestBody TeamRequest request) {
        return ResponseEntity.ok(teamService.createTeam(request));
    }
}
