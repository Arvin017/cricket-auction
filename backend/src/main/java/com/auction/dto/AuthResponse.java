package com.auction.dto;

import com.auction.entity.UserRole;

public record AuthResponse(
        String token,
        Long userId,
        String username,
        UserRole role,
        Long teamId,
        String teamName
) {}
