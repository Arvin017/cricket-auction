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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds the database with a realistic demo set of teams, players, and login
 * accounts so the app can be demoed immediately without manual data entry.
 *
 * Each piece (teams, players, users) is seeded independently and checked
 * separately, rather than gating everything on "do teams exist yet" — that
 * used to mean a partial failure (e.g. teams inserted but players failing)
 * would permanently skip re-seeding the missing piece on every future
 * restart. The whole thing also runs in one transaction, so a failure
 * partway through rolls back cleanly instead of leaving a half-seeded
 * database behind.
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
    @Transactional
    public void run(String... args) {
        List<Team> teams = seedTeams();
        seedPlayers();
        seedUsers(teams);
    }

    private List<Team> seedTeams() {
        if (teamRepository.count() > 0) {
            log.info("Teams already seeded ({}), skipping.", teamRepository.count());
            return teamRepository.findAll();
        }

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
        log.info("Seeded {} teams.", teams.size());
        return teams;
    }

    private void seedPlayers() {
        if (playerRepository.count() > 0) {
            log.info("Players already seeded ({}), skipping.", playerRepository.count());
            return;
        }

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
                p("Suresh Pillai", PlayerRole.BATSMAN, "India", 10000000L, "260 runs, avg 26.0"),
                p("Dhruv Malhotra", PlayerRole.BATSMAN, "India", 16000000L, "340 runs, avg 31.5"),
                p("Connor Blake", PlayerRole.BOWLER, "England", 13500000L, "26 wickets, econ 7.6"),
                p("Yuvan Rao", PlayerRole.ALL_ROUNDER, "India", 22000000L, "245 runs, 21 wkts"),
                p("Zaid Qureshi", PlayerRole.WICKETKEEPER, "India", 12000000L, "230 runs, 16 dismissals"),
                p("Ryan Fletcher", PlayerRole.BOWLER, "Australia", 19000000L, "37 wickets, econ 7.0"),
                p("Karthik Iyer", PlayerRole.BATSMAN, "India", 14000000L, "310 runs, avg 28.2"),
                p("Josh Bannerman", PlayerRole.ALL_ROUNDER, "New Zealand", 21000000L, "200 runs, 18 wkts"),
                p("Aman Chauhan", PlayerRole.BOWLER, "India", 10000000L, "22 wickets, econ 7.9"),
                p("Ollie Marsh", PlayerRole.BATSMAN, "England", 20000000L, "398 runs, SR 145"),
                p("Nikhil Bhatt", PlayerRole.WICKETKEEPER, "India", 13000000L, "255 runs, 15 dismissals"),
                p("Dwayne Clarke", PlayerRole.ALL_ROUNDER, "West Indies", 24000000L, "280 runs, 19 wkts"),
                p("Harsh Vardhan", PlayerRole.BOWLER, "India", 11000000L, "25 wickets, econ 7.3"),
                p("Ethan Cross", PlayerRole.BATSMAN, "South Africa", 17000000L, "330 runs, avg 30.0"),
                p("Mihir Solanki", PlayerRole.ALL_ROUNDER, "India", 15000000L, "190 runs, 16 wkts"),
                p("Callum Reid", PlayerRole.BOWLER, "Australia", 14500000L, "28 wickets, econ 7.5"),
                p("Adarsh Menon", PlayerRole.WICKETKEEPER, "India", 10000000L, "180 runs, 11 dismissals"),
                p("George Pemberton", PlayerRole.BATSMAN, "England", 23000000L, "410 runs, SR 141"),
                p("Rohit Bhagat", PlayerRole.BOWLER, "India", 12500000L, "24 wickets, econ 7.7"),
                p("Danesh Pillay", PlayerRole.ALL_ROUNDER, "South Africa", 19500000L, "215 runs, 17 wkts")
        ));
        log.info("Seeded {} players.", playerRepository.count());
    }

    private void seedUsers(List<Team> teams) {
        if (userRepository.count() > 0) {
            log.info("Users already seeded ({}), skipping.", userRepository.count());
            return;
        }

        userRepository.save(User.builder()
                .username("admin")
                .email("admin@auction.demo")
                .password(passwordEncoder.encode("admin123"))
                .role(UserRole.AUCTIONEER)
                .build());

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

        log.info("Seeded {} users. Login as admin/admin123 (auctioneer) or e.g. mumbaimonarchs/team123 (team rep).",
                userRepository.count());
    }

    private Player p(String name, PlayerRole role, String nat, Long basePrice, String stats) {
        return Player.builder()
                .name(name).role(role).nationality(nat).basePrice(basePrice).stats(stats)
                .status(PlayerStatus.UPCOMING)
                .photoUrl("https://api.dicebear.com/7.x/personas/svg?seed=" + name.replaceAll("\\s+", ""))
                .build();
    }
}