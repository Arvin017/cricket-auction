package com.auction.repository;

import com.auction.entity.AuctionSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionSessionRepository extends JpaRepository<AuctionSession, Long> {
}
