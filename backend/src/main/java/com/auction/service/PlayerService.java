package com.auction.service;

import com.auction.dto.PlayerRequest;
import com.auction.entity.Player;
import com.auction.entity.PlayerStatus;
import com.auction.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;

    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }

    public Player getPlayer(Long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + id));
    }

    public List<Player> getUpcomingPlayers() {
        return playerRepository.findByStatusOrderById(PlayerStatus.UPCOMING);
    }

    public Player addPlayer(PlayerRequest request) {
        Player player = Player.builder()
                .name(request.name())
                .role(request.role())
                .nationality(request.nationality())
                .basePrice(request.basePrice())
                .stats(request.stats())
                .photoUrl(request.photoUrl())
                .status(PlayerStatus.UPCOMING)
                .build();
        return playerRepository.save(player);
    }

    public Player save(Player player) {
        return playerRepository.save(player);
    }
}
