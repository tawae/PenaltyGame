package penaltyclient.controller;

import javafx.application.Platform;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import penaltyclient.view.MatchView1;
import network.ClientNetwork;

public class MatchController {
    private MatchView1 matchView;
    private ClientNetwork network;
    private String playerName;
    private String opponentName;
    
    // Game state
    private boolean isMyTurn;
    private String myRole; // "SHOOTER" or "GOALKEEPER"
    private int myScore = 0;
    private int opponentScore = 0;
    private int currentRound = 1;
    private int maxRounds = 5;
    
    // Turn timing
    private Timeline turnTimer;
    private int remainingSeconds = 10;
    private int selectedZone = -1;
    
    public MatchController(MatchView1 view, ClientNetwork network, String playerName) {
        this.matchView = view;
        this.network = network;
        this.playerName = playerName;
        
        // Set controller reference in view
        matchView.setController(this);
        
        // Setup network message handlers
        setupNetworkHandlers();
    }
    
    private void setupNetworkHandlers() {
        network.setMessageHandler(message -> {
            Platform.runLater(() -> handleServerMessage(message));
        });
    }
    
    public void handleConnect(Stage primaryStage, String playerName) {
        if (network.connect(playerName)) {
            matchView.initializeGameUI(primaryStage);
            network.joinQueue();
            matchView.updateMessage("Waiting for opponent...");
        } else {
            matchView.showErrorDialog("Could not connect to server!");
            Platform.exit();
        }
    }

    private void handleServerMessage(String message) {
        String[] parts = message.split("\\|");
        String command = parts[0];
        
        switch (command) {
            case "MATCH_START":
                handleMatchStart(parts);
                break;
            case "TURN_START":
                handleTurnStart(parts);
                break;
            case "TURN_RESULT":
                handleTurnResult(parts);
                break;
            case "MATCH_END":
                handleMatchEnd(parts);
                break;
            case "OPPONENT_DISCONNECTED":
                handleOpponentDisconnected();
                break;
        }
    }
    
    private void handleMatchStart(String[] parts) {
        // MATCH_START|opponentName|firstShooter
        opponentName = parts[1];
        String firstShooter = parts[2];
        
        isMyTurn = firstShooter.equals(playerName);
        myRole = isMyTurn ? "SHOOTER" : "GOALKEEPER";
        
        matchView.showMatchStartMessage(
            "Match started against " + opponentName + "!\n" +
            (isMyTurn ? "You shoot first!" : "Opponent shoots first!")
        );
        
        // Wait 3 seconds then start first turn
        Timeline delay = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            if (isMyTurn) {
                startMyTurn();
            } else {
                waitForOpponent();
            }
        }));
        delay.play();
    }
    
    private void handleTurnStart(String[] parts) {
        // TURN_START|round|role
        currentRound = Integer.parseInt(parts[1]);
        myRole = parts[2];
        isMyTurn = true;
        
        startMyTurn();
    }
    
    private void startMyTurn() {
        selectedZone = -1;
        remainingSeconds = 10;
        
        if (myRole.equals("SHOOTER")) {
            matchView.enableShooterMode();
            matchView.updateMessage("Your turn to SHOOT! Select a zone (" + remainingSeconds + "s)");
        } else {
            matchView.enableGoalkeeperMode();
            matchView.updateMessage("Your turn to SAVE! Predict shooter's zone (" + remainingSeconds + "s)");
        }
        
        startTurnTimer();
    }
    
    private void startTurnTimer() {
        if (turnTimer != null) {
            turnTimer.stop();
        }
        
        turnTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingSeconds--;
            
            if (remainingSeconds > 0) {
                String action = myRole.equals("SHOOTER") ? "SHOOT" : "SAVE";
                matchView.updateMessage("Your turn to " + action + "! (" + remainingSeconds + "s)");
            } else {
                // Time's up - submit choice or random
                submitChoice();
            }
        }));
        turnTimer.setCycleCount(10);
        turnTimer.play();
    }
    
    // Called by MatchView when player selects a zone
    public void onZoneSelected(int zoneIndex) {
        if (!isMyTurn) return;
        
        selectedZone = zoneIndex;
        String[] zoneNames = {"Top Left", "Top Center", "Top Right", 
                             "Bottom Left", "Bottom Center", "Bottom Right"};
        matchView.updateMessage("Selected: " + zoneNames[zoneIndex] + " - Press SPACE to confirm!");
    }
    
    // Called by MatchView when player confirms (SPACE key or SHOOT button)
    public void onConfirmChoice() {
        if (!isMyTurn) return;
        
        submitChoice();
    }
    
    private void submitChoice() {
        if (turnTimer != null) {
            turnTimer.stop();
        }
        
        // If no zone selected, choose random
        if (selectedZone == -1) {
            selectedZone = (int)(Math.random() * 6);
        }
        
        // Disable further input
        isMyTurn = false;
        matchView.disableInput();
        matchView.updateMessage("Waiting for result...");
        
        // Send choice to server
        network.sendMessage("CHOICE|" + selectedZone);
        
        // Wait for opponent and result
        waitForOpponent();
    }
    
    private void waitForOpponent() {
        matchView.disableInput();
        matchView.updateMessage("Waiting for opponent...");
    }
    
    private void handleTurnResult(String[] parts) {
        // TURN_RESULT|shooterZone|keeperZone|isGoal|myNewScore|opponentNewScore|shooterName
        int shooterZone = Integer.parseInt(parts[1]);
        int keeperZone = Integer.parseInt(parts[2]);
        boolean isGoal = Boolean.parseBoolean(parts[3]);
        myScore = Integer.parseInt(parts[4]);
        opponentScore = Integer.parseInt(parts[5]);
        String shooterName = parts[6];
        
        // Update score display
        matchView.updateScore(myScore, opponentScore);
        
        // Determine who was shooter in this turn
        boolean iWasShooter = shooterName.equals(playerName);
        
        // Show animation
        if (iWasShooter) {
            matchView.playShootAnimation(shooterZone, keeperZone, isGoal, result -> {
                showTurnResultMessage(isGoal, true);
                prepareNextTurn();
            });
        } else {
            matchView.playGoalkeeperAnimation(shooterZone, keeperZone, isGoal, result -> {
                showTurnResultMessage(isGoal, false);
                prepareNextTurn();
            });
        }
    }
    
    private void showTurnResultMessage(boolean isGoal, boolean iWasShooter) {
        if (iWasShooter) {
            if (isGoal) {
                matchView.updateMessage("GOAL! You scored!");
            } else {
                matchView.updateMessage("SAVED! Opponent blocked your shot!");
            }
        } else {
            if (isGoal) {
                matchView.updateMessage("GOAL! Opponent scored!");
            } else {
                matchView.updateMessage("SAVED! You blocked the shot!");
            }
        }
    }
    
    private void prepareNextTurn() {
        // Wait 2 seconds before next turn
        Timeline delay = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            matchView.resetField();
            // Server will send TURN_START for next turn
        }));
        delay.play();
    }
    
    private void handleMatchEnd(String[] parts) {
        // MATCH_END|winner|finalMyScore|finalOpponentScore
        String winner = parts[1];
        int finalMyScore = Integer.parseInt(parts[2]);
        int finalOpponentScore = Integer.parseInt(parts[3]);
        
        matchView.updateScore(finalMyScore, finalOpponentScore);
        
        String resultMessage;
        if (winner.equals(playerName)) {
            resultMessage = "🎉 YOU WIN! 🎉\n" +
                          "Final Score: " + finalMyScore + " - " + finalOpponentScore;
        } else if (winner.equals("DRAW")) {
            resultMessage = "Match continues to sudden death!";
            return; // Continue playing
        } else {
            resultMessage = "You Lost!\n" +
                          "Final Score: " + finalMyScore + " - " + finalOpponentScore;
        }
        
        matchView.showMatchEndMessage(resultMessage);
    }
    
    private void handleOpponentDisconnected() {
        if (turnTimer != null) {
            turnTimer.stop();
        }
        matchView.showErrorMessage("Opponent disconnected! You win by default.");
    }
    
    public void requestRematch() {
        network.sendMessage("REMATCH_REQUEST");
    }
    
    public void leaveMatch() {
        if (turnTimer != null) {
            turnTimer.stop();
        }
        network.sendMessage("LEAVE_MATCH");
    }
}