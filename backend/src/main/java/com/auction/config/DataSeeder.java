package com.auction.config;

import com.auction.entity.*;
import com.auction.repository.PlayerRepository;
import com.auction.repository.TeamRepository;
import com.auction.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the database with a realistic demo set of teams, players, and login
 * accounts so the app can be demoed immediately without manual data entry.
 * Only runs if the teams table is empty (safe to restart the app).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (teamRepository.count() > 0) {
            log.info("Data already seeded, skipping.");
            return;
        }

        log.info("Seeding demo data...");

        List<Team> teams = teamRepository.saveAll(List.of(
                Team.builder().name("Mumbai Monarchs").logoUrl("https://api.dicebear.com/7.x/shapes/svg?seed=Mumbai")
                        .purseTotal(1000000000L).purseRemaining(1000000000L).squadSizeMax(25).squadSizeMin(18).build(),
                Team.builder().name("Chennai Chargers").logoUrl("https://api.dicebear.com/7.x/shapes/svg?seed=Chennai")
                        .purseTotal(1000000000L).purseRemaining(1000000000L).squadSizeMax(25).squadSizeMin(18).build(),
                Team.builder().name("Bengaluru Blasters").logoUrl("https://api.dicebear.com/7.x/shapes/svg?seed=Bengaluru")
                        .purseTotal(1000000000L).purseRemaining(1000000000L).squadSizeMax(25).squadSizeMin(18).build(),
                Team.builder().name("Delhi Dominators").logoUrl("https://api.dicebear.com/7.x/shapes/svg?seed=Delhi")
                        .purseTotal(1000000000L).purseRemaining(1000000000L).squadSizeMax(25).squadSizeMin(18).build(),
                Team.builder().name("Kolkata Knightriders XI").logoUrl("https://api.dicebear.com/7.x/shapes/svg?seed=Kolkata")
                        .purseTotal(1000000000L).purseRemaining(1000000000L).squadSizeMax(25).squadSizeMin(18).build(),
                Team.builder().name("Hyderabad Hurricanes").logoUrl("https://api.dicebear.com/7.x/shapes/svg?seed=Hyderabad")
                        .purseTotal(1000000000L).purseRemaining(1000000000L).squadSizeMax(25).squadSizeMin(18).build()
        ));

        // Purse total: ₹100 Cr = 1,000,000,000 rupees, matching the project spec.

        playerRepository.saveAll(List.of(
                p("Rohan Verma", PlayerRole.BATSMAN, "India", 20000000L, "412 runs, avg 34.2, SR 148"),
                p("Jasdeep Singh", PlayerRole.BOWLER, "India", 15000000L, "38 wickets, econ 7.1"),
                p("Marcus Cole", PlayerRole.ALL_ROUNDER, "Australia", 30000000L, "298 runs, 22 wkts"),
                p("Arjun Nair", PlayerRole.WICKETKEEPER, "India", 10000000L, "312 runs, 14 dismissals"),
                p("Ben Wickham", PlayerRole.BOWLER, "England", 12500000L, "29 wickets, econ 8.0"),
                p("Kabir Sharma", PlayerRole.BATSMAN, "India", 25000000L, "501 runs, avg 41.7"),
                p("Trent Falcon", PlayerRole.ALL_ROUNDER, "New Zealand", 18000000L, "220 runs, 19 wkts"),
                p("Devendra Yadav", PlayerRole.BOWLER, "India", 10000000L, "31 wickets, econ 6.9"),
                p("Sam Ridley", PlayerRole.BATSMAN, "South Africa", 22000000L, "389 runs, SR 152"),
                p("Vikram Rathore Jr.", PlayerRole.WICKETKEEPER, "India", 15000000L, "276 runs, 18 dismissals"),
                p("Ishaan Kulkarni", PlayerRole.ALL_ROUNDER, "India", 20000000L, "180 runs, 24 wkts"),
                p("Michael Draper", PlayerRole.BOWLER, "Australia", 16000000L, "34 wickets, econ 7.4"),
                p("Rahul Deshmukh", PlayerRole.BATSMAN, "India", 12000000L, "298 runs, avg 29.8"),
                p("Chris Okafor", PlayerRole.ALL_ROUNDER, "West Indies", 25000000L, "310 runs, 20 wkts"),
                p("Naveen Reddy", PlayerRole.BOWLER, "India", 10000000L, "27 wickets, econ 7.8"),
                p("Tom Ashworth", PlayerRole.BATSMAN, "England", 18000000L, "420 runs, SR 139"),
                p("Aditya Bhosale", PlayerRole.WICKETKEEPER, "India", 10000000L, "199 runs, 12 dismissals"),
                p("Faisal Ahmed", PlayerRole.BOWLER, "Pakistan-eligible*", 14000000L, "30 wickets, econ 7.2"),
                p("Liam O'Sullivan", PlayerRole.ALL_ROUNDER, "Ireland", 10000000L, "150 runs, 15 wkts"),
                p("Suresh Pillai", PlayerRole.BATSMAN, "India", 10000000L, "260 runs, avg 26.0")
        ));

        // Auctioneer account
        userRepository.save(User.builder()
                .username("admin")
                .email("admin@auction.demo")
                .password(passwordEncoder.encode("admin123"))
                .role(UserRole.AUCTIONEER)
                .build());

        // One demo team-rep login per team (password: team123)
        for (Team team : teams) {
            String uname = team.getName().toLowerCase().replaceAll("[^a-z]", "");
            userRepository.save(User.builder()
                    .username(uname)
                    .email(uname + "@auction.demo")
                    .password(passwordEncoder.encode("team123"))
                    .role(UserRole.TEAM_REP)
                    .team(team)
                    .build());
        }

        log.info("Seed complete: {} teams, {} players, {} users. Login as admin/admin123 (auctioneer) or e.g. mumbaimonarchs/team123 (team rep).",
                teams.size(), playerRepository.count(), userRepository.count());
    }

    private Player p(String name, PlayerRole role, String nat, Long basePrice, String stats) {
        return Player.builder()
                .name(name).role(role).nationality(nat).basePrice(basePrice).stats(stats)
                .status(PlayerStatus.UPCOMING)
                .photoUrl("https://api.dicebear.com/7.x/personas/svg?seed=" + name.replaceAll("\\s+", ""))
                .build();
    }
}
