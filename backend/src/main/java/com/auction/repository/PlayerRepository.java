package com.auction.repository;

import com.auction.entity.Player;
import com.auction.entity.PlayerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    List<Player> findByStatus(PlayerStatus status);
    List<Player> findByStatusOrderById(PlayerStatus status);
}
