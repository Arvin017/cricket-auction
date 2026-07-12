package com.auction.service;

import com.auction.dto.AuthResponse;
import com.auction.dto.LoginRequest;
import com.auction.dto.RegisterRequest;
import com.auction.entity.Team;
import com.auction.entity.User;
import com.auction.entity.UserRole;
import com.auction.repository.TeamRepository;
import com.auction.repository.UserRepository;
import com.auction.security.CustomUserDetails;
import com.auction.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        Team team = null;
        if (request.role() == UserRole.TEAM_REP) {
            if (request.teamId() == null) {
                throw new IllegalArgumentException("teamId is required for TEAM_REP registration");
            }
            team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> new IllegalArgumentException("Team not found: " + request.teamId()));
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .team(team)
                .build();

        user = userRepository.save(user);

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("userId", user.getId());
        if (user.getTeam() != null) {
            claims.put("teamId", user.getTeam().getId());
        }

        String token = jwtUtil.generateToken(userDetails, claims);

        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getTeam() != null ? user.getTeam().getId() : null,
                user.getTeam() != null ? user.getTeam().getName() : null
        );
    }
}
