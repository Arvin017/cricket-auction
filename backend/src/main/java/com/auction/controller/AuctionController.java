package com.auction.controller;

import com.auction.dto.AuctionStateMessage;
import com.auction.dto.BidRequest;
import com.auction.entity.User;
import com.auction.security.CustomUserDetails;
import com.auction.service.AuctionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;

    // ---- Auctioneer/admin controls ----

    @PostMapping("/api/admin/auction/start/{playerId}")
    @PreAuthorize("hasRole('AUCTIONEER')")
    public ResponseEntity<AuctionStateMessage> start(@PathVariable Long playerId) {
        return ResponseEntity.ok(auctionService.startPlayer(playerId));
    }

    @PostMapping("/api/admin/auction/pause")
    @PreAuthorize("hasRole('AUCTIONEER')")
    public ResponseEntity<AuctionStateMessage> pause() {
        return ResponseEntity.ok(auctionService.pause());
    }

    @PostMapping("/api/admin/auction/resume")
    @PreAuthorize("hasRole('AUCTIONEER')")
    public ResponseEntity<AuctionStateMessage> resume() {
        return ResponseEntity.ok(auctionService.resume());
    }

    @PostMapping("/api/admin/auction/finalize")
    @PreAuthorize("hasRole('AUCTIONEER')")
    public ResponseEntity<AuctionStateMessage> finalizeSale() {
        return ResponseEntity.ok(auctionService.finalizeCurrentPlayer());
    }

    @GetMapping("/api/auction/state")
    public ResponseEntity<Map<Object, Object>> getState() {
        return ResponseEntity.ok(auctionService.getCurrentState());
    }

    // ---- Team rep bidding ----

    @PostMapping("/api/auction/bid")
    @PreAuthorize("hasRole('TEAM_REP')")
    public ResponseEntity<AuctionStateMessage> placeBid(@Valid @RequestBody BidRequest request,
                                                          @AuthenticationPrincipal CustomUserDetails principal) {
        User user = principal.getUser();
        if (user.getTeam() == null) {
            throw new IllegalStateException("Your account is not linked to a team");
        }
        AuctionStateMessage result = auctionService.placeBid(request.playerId(), user.getTeam().getId(), request.amount());
        return ResponseEntity.ok(result);
    }
}
