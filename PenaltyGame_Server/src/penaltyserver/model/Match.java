/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package penaltyserver.model;

import java.sql.Timestamp;
import java.util.*;

/**
 *
 * @author This PC
 */
public class Match {
    private String matchId;
    private String matchStatus;
    private int createdBy;
    private Timestamp startTime;
    private Timestamp endTime;

    private User player1;
    private User player2;
    private transient ClientHandler handler1; // transient: không lưu vào DB nếu dùng ORM
    private transient ClientHandler handler2; // transient: không lưu vào DB nếu dùng ORM
    private User currentShooter;
    private User currentKeeper;
    private int player1Score = 0;
    private int player2Score = 0;
    private int currentRound = 1;
    private int maxNormalRounds = 5;
    private boolean isSuddenDeath = false;
    private Integer shooterChoice = null;
    private Integer keeperChoice = null;
    private transient Timer turnTimer;
    
    public Match(String matchId, User player1, User player2, ClientHandler handler1, ClientHandler handler2) {
        this.matchId = matchId;
        this.player1 = player1;
        this.player2 = player2;
        this.handler1 = handler1;
        this.handler2 = handler2;
        this.matchStatus = "playing"; // Bắt đầu là playing
        this.startTime = new Timestamp(System.currentTimeMillis()); // Ghi lại thời gian bắt đầu
    }
    public String getMatchId() {
        return matchId;
    }

    public String getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(String matchStatus) {
        this.matchStatus = matchStatus;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }

    public User getPlayer1() {
        return player1;
    }

    public User getPlayer2() {
        return player2;
    }

    public ClientHandler getHandler1() {
        return handler1;
    }

    public ClientHandler getHandler2() {
        return handler2;
    }

    public User getCurrentShooter() {
        return currentShooter;
    }

    public void setCurrentShooter(User currentShooter) {
        this.currentShooter = currentShooter;
    }

    public User getCurrentKeeper() {
        return currentKeeper;
    }

    public void setCurrentKeeper(User currentKeeper) {
        this.currentKeeper = currentKeeper;
    }

    public int getPlayer1Score() {
        return player1Score;
    }

    public void setPlayer1Score(int player1Score) {
        this.player1Score = player1Score;
    }

    public int getPlayer2Score() {
        return player2Score;
    }

    public void setPlayer2Score(int player2Score) {
        this.player2Score = player2Score;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public int getMaxNormalRounds() {
        return maxNormalRounds;
    }

    public boolean isSuddenDeath() {
        return isSuddenDeath;
    }

    public void setSuddenDeath(boolean suddenDeath) {
        isSuddenDeath = suddenDeath;
    }

    public Integer getShooterChoice() {
        return shooterChoice;
    }

    public void setShooterChoice(Integer shooterChoice) {
        this.shooterChoice = shooterChoice;
    }

    public Integer getKeeperChoice() {
        return keeperChoice;
    }

    public void setKeeperChoice(Integer keeperChoice) {
        this.keeperChoice = keeperChoice;
    }

    public Timer getTurnTimer() {
        return turnTimer;
    }

    public void setTurnTimer(Timer turnTimer) {
        this.turnTimer = turnTimer;
    }
    
    public void sendToPlayer(User player, String message) {
        ClientHandler handler = (player.equals(player1)) ? handler1 : handler2;
        if (handler != null) {
            handler.sendMessage(message);
        } else {
            System.err.println("Error sending message in Match " + matchId + ": Handler not found for player " + player.getUsername());
            // Cân nhắc gọi handleDisconnect trong Controller từ đây nếu handler null
        }
    }

    // Lấy người chơi còn lại
    public User getOtherPlayer(User player) {
        return player.equals(player1) ? player2 : player1;
    }
    
    public void incrementScore(User player) {
        if (player.equals(player1)) {
            player1Score++;
        } else if (player.equals(player2)) {
            player2Score++;
        }
    }

    // Đổi vai trò (logic nhỏ có thể giữ lại)
    public void swapRoles() {
        User temp = currentShooter;
        currentShooter = currentKeeper;
        currentKeeper = temp;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Match match = (Match) o;
        return Objects.equals(matchId, match.matchId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(matchId);
    }
}