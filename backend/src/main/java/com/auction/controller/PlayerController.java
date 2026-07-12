package com.auction.controller;

import com.auction.dto.PlayerRequest;
import com.auction.entity.Player;
import com.auction.service.PlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    @GetMapping
    public ResponseEntity<List<Player>> getAllPlayers() {
        return ResponseEntity.ok(playerService.getAllPlayers());
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<Player>> getUpcoming() {
        return ResponseEntity.ok(playerService.getUpcomingPlayers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Player> getPlayer(@PathVariable Long id) {
        return ResponseEntity.ok(playerService.getPlayer(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('AUCTIONEER')")
    public ResponseEntity<Player> addPlayer(@Valid @RequestBody PlayerRequest request) {
        return ResponseEntity.ok(playerService.addPlayer(request));
    }
}
