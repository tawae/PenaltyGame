package penaltyserver.controller;

import java.io.ObjectOutputStream;
import java.util.*;
import penaltyserver.PenaltyServer;
import penaltyserver.model.User;

public class MatchController {
    private PenaltyServer server;
    private Map<String, Match> activeMatches;
    private Queue<User> waitingPlayers;
    
    public MatchController(PenaltyServer server) {
        this.server = server;
        this.activeMatches = new HashMap<>();
        this.waitingPlayers = new LinkedList<>();
    }
    
    public void handlePlayerJoinQueue(User user) {
        waitingPlayers.add(user);
        System.out.println("User " + user.getUsername() + " joined queue. Queue size: " + waitingPlayers.size());
        
        // Try to create a match if we have 2 players
        if (waitingPlayers.size() >= 2) {
            createMatch();
        }
    }
    
    private void createMatch() {
        User player1 = waitingPlayers.poll();
        User player2 = waitingPlayers.poll();
        
        if (player1 == null || player2 == null) return;
        
        String matchId = UUID.randomUUID().toString();
        Match match = new Match(matchId, player1, player2);
        
        activeMatches.put(matchId, match);
        player1.setMatchId(matchId);
        player2.setMatchId(matchId);
        
        System.out.println("Match created: " + player1.getUsername() + " vs " + player2.getUsername());
        
        // Start match
        match.startMatch();
    }
    
    public void handlePlayerChoice(User user, int zoneChoice) {
        String matchId = user.getMatchId();
        Match match = activeMatches.get(matchId);
        
        if (match != null) {
            match.registerChoice(user, zoneChoice);
        }
    }
    
    public void handlePlayerDisconnect(User user) {
        String matchId = user.getMatchId();
        if (matchId != null) {
            Match match = activeMatches.get(matchId);
            if (match != null) {
                match.handleDisconnect(user);
                activeMatches.remove(matchId);
            }
        }
        
        // Remove from waiting queue if present
        waitingPlayers.remove(user);
    }
    
    // Inner class representing a match between two players
    private class Match {
        private String matchId;
        private User player1;
        private User player2;
        
        private User currentShooter;
        private User currentKeeper;
        
        private int player1Score = 0;
        private int player2Score = 0;
        private int currentRound = 1;
        private int maxRounds = 5;
        
        private Integer shooterChoice = null;
        private Integer keeperChoice = null;
        
        public Match(String matchId, User player1, User player2) {
            this.matchId = matchId;
            this.player1 = player1;
            this.player2 = player2;
        }
        
        public void startMatch() {
            // Randomly choose who shoots first
            if (Math.random() < 0.5) {
                currentShooter = player1;
                currentKeeper = player2;
            } else {
                currentShooter = player2;
                currentKeeper = player1;
            }
            
            // Notify both players
            server.sendToPlayer(player1, "MATCH_START|" + player2.getUsername() + "|" + currentShooter.getUsername());
            server.sendToPlayer(player2, "MATCH_START|" + player1.getUsername() + "|" + currentShooter.getUsername());
            
            System.out.println("Match started: " + currentShooter.getUsername() + " shoots first");
            
            // Start first turn after delay
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            startTurn();
        }
        
        private void startTurn() {
            shooterChoice = null;
            keeperChoice = null;
            
            // Notify shooter
            server.sendToPlayer(currentShooter, "TURN_START|" + currentRound + "|SHOOTER");
            
            // Notify keeper
            server.sendToPlayer(currentKeeper, "TURN_START|" + currentRound + "|GOALKEEPER");
            
            System.out.println("Round " + currentRound + ": " + currentShooter.getUsername() + " shoots, " + currentKeeper.getUsername() + " keeps");
            
            // Start timer for choices (10 seconds)
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    // Auto-submit random choices if not received
                    synchronized (Match.this) {
                        if (shooterChoice == null) {
                            shooterChoice = (int)(Math.random() * 6);
                            System.out.println(currentShooter.getUsername() + " timeout - random zone: " + shooterChoice);
                        }
                        if (keeperChoice == null) {
                            keeperChoice = (int)(Math.random() * 6);
                            System.out.println(currentKeeper.getUsername() + " timeout - random zone: " + keeperChoice);
                        }
                        
                        if (shooterChoice != null && keeperChoice != null) {
                            processTurn();
                        }
                    }
                }
            }, 10000);
        }
        
        public synchronized void registerChoice(User user, int zone) {
            if (user.equals(currentShooter)) {
                if (shooterChoice == null) {
                    shooterChoice = zone;
                    System.out.println(currentShooter.getUsername() + " chose zone: " + zone);
                }
            } else if (user.equals(currentKeeper)) {
                if (keeperChoice == null) {
                    keeperChoice = zone;
                    System.out.println(currentKeeper.getUsername() + " chose zone: " + zone);
                }
            }
            
            // If both choices received, process immediately
            if (shooterChoice != null && keeperChoice != null) {
                processTurn();
            }
        }
        
        private void processTurn() {
            // Determine if goal or save
            boolean isGoal = (shooterChoice != keeperChoice);
            
            // Update score
            if (isGoal) {
                if (currentShooter.equals(player1)) {
                    player1Score++;
                } else {
                    player2Score++;
                }
            }
            
            System.out.println("Result: Shooter zone " + shooterChoice + ", Keeper zone " + keeperChoice + " -> " + (isGoal ? "GOAL" : "SAVE"));
            System.out.println("Score: " + User1.getUsername() + " " + player1Score + " - " + player2Score + " " + player2.getUsername());
            
            // Send result to both players
            // Format: TURN_RESULT|shooterZone|keeperZone|isGoal|myScore|opponentScore|shooterName
            String resultP1 = "TURN_RESULT|" + shooterChoice + "|" + keeperChoice + "|" + isGoal + "|" + 
                            player1Score + "|" + player2Score + "|" + currentShooter.getUsername();
            String resultP2 = "TURN_RESULT|" + shooterChoice + "|" + keeperChoice + "|" + isGoal + "|" + 
                            player2Score + "|" + player1Score + "|" + currentShooter.getUsername();
            
            server.sendToPlayer(player1, resultP1);
            server.sendToPlayer(player2, resultP2);
            
            // Wait for animation to finish
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // Swap shooter and keeper for next turn
            User temp = currentShooter;
            currentShooter = currentKeeper;
            currentKeeper = temp;
            
            // After both players have had a turn, increment round
            // User who started second will shoot again = new round starts
            if (currentShooter.equals(player1) ? currentRound > 1 : currentRound >= 1) {
                currentRound++;
            }
            
            // Check if we've completed all rounds
            if (currentRound > maxRounds) {
                checkMatchEnd();
            } else {
                startTurn();
            }
        }
        
        private void checkMatchEnd() {
            // After 5 rounds (10 turns), check winner
            if (player1Score > player2Score) {
                endMatch(player1);
            } else if (player2Score > player1Score) {
                endMatch(player2);
            } else {
                // Tied - sudden death
                System.out.println("Match tied! Going to sudden death...");
                maxRounds++;
                startTurn();
            }
        }
        
        private void endMatch(User winner) {
            System.out.println("Match ended! Winner: " + winner.getUsername());
            
            // Notify both players
            server.sendToPlayer(player1, "MATCH_END|" + winner.getUsername() + "|" + 
                              player1Score + "|" + player2Score);
            server.sendToPlayer(player2, "MATCH_END|" + winner.getUsername() + "|" + 
                              player2Score + "|" + player1Score);
            
            // Clean up
            player1.setMatchId(null);
            player2.setMatchId(null);
            activeMatches.remove(matchId);
        }
        
        public void handleDisconnect(User disconnectedPlayer) {
            User otherPlayer = disconnectedPlayer.equals(player1) ? player2 : player1;
            
            // Notify other user
            server.sendToPlayer(otherPlayer, "OPPONENT_DISCONNECTED");
            
            // End match
            System.out.println("User " + disconnectedPlayer.getUsername() + " disconnected from match");
            
            otherPlayer.setMatchId(null);
        }
    }
}