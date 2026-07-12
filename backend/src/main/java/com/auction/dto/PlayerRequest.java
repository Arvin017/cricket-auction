package com.auction.dto;

import com.auction.entity.PlayerRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PlayerRequest(
        @NotBlank String name,
        @NotNull PlayerRole role,
        String nationality,
        @NotNull @Positive Long basePrice,
        String stats,
        String photoUrl
) {}
