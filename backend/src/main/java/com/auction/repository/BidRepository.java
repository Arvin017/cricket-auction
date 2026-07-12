package com.auction.repository;

import com.auction.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByPlayerIdOrderByTimestampDesc(Long playerId);
    List<Bid> findByTeamIdOrderByTimestampDesc(Long teamId);
}
