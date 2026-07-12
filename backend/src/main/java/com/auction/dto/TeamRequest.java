package com.auction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TeamRequest(
        @NotBlank String name,
        String logoUrl,
        @NotNull @Positive Long purseTotal,
        Integer squadSizeMax,
        Integer squadSizeMin
) {}
