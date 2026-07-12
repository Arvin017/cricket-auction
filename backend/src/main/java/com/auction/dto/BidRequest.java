package com.auction.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BidRequest(
        @NotNull Long playerId,
        @NotNull @Positive Long amount
) {}
