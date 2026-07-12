package com.auction.dto;

import com.auction.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotNull UserRole role,
        // Required only when role == TEAM_REP
        Long teamId
) {}
