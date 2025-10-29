package penaltyclient.controller;

import javafx.application.Platform;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import penaltyclient.view.MatchView;
import javafx.stage.Stage;
import penaltyclient.model.ClientListener;
//import network.ClientNetwork;

public class MatchController {
    private MatchView matchView;

    private ClientListener clientListener;
    private LobbyController lobbyController;
    private Stage matchStage;
    
    private String playerName;
    private String opponentName;
    
    // Game state
    private boolean isMyTurn = false;
    private String myRole; // "SHOOTER" or "GOALKEEPER"
    private int myScore = 0;
    private int opponentScore = 0;
    private int currentRound = 1;
    private int maxRounds = 5;
    
    // Turn timing
    private Timeline turnTimer;
    private int remainingSeconds = 10;
    private int selectedZone = -1;
    private boolean choiceConfirmed = false;
    
    public MatchController(String matchId, String playerName, String opponentName, ClientListener clientListener) {
//        this.matchView = view;
        this.clientListener = clientListener;
        this.playerName = playerName;
        this.opponentName = opponentName;
        
//        matchView.setController(this);
    }
    
    public void showMatchView() {
        matchStage = new Stage(); // Tạo cửa sổ mới
        matchView = new MatchView(); // Tạo instance của MatchView (Application)
        matchView.setController(this); // Liên kết View với Controller này
        try {
            matchView.start(matchStage); // Khởi chạy UI JavaFX trong cửa sổ mới
            updateViewScores(); // Cập nhật điểm ban đầu
            matchView.updateMessage("Waiting for match start...");
        } catch (Exception e) {
            e.printStackTrace();
            // Xử lý lỗi nếu không khởi tạo được View
        }
    }
    private void closeMatchView() {
        if (matchStage != null) {
            matchStage.close();
        }
        
        lobbyController.showLobbyView();
    }

    public void handleServerMessage(String message) {
        System.out.println("Nhan duoc tu server: " + message);
        String[] parts = message.split("\\:");
        String command = parts[0];
        
        Platform.runLater(() -> {
            switch (command) {
                case "MATCH_START": // Server gửi: MATCH_START:<opponentName>:<firstShooter>
                    if (parts.length >= 3) {
                         // Cập nhật lại tên đối thủ phòng trường hợp START_MATCH không gửi
                        this.opponentName = parts[1];
                        String firstShooter = parts[2];
                        handleMatchStart(firstShooter);
                    }
                    break;
                case "TURN_START": // Server gửi: TURN_START:<round>:<your_role>
                    if (parts.length >= 3) {
                        currentRound = Integer.parseInt(parts[1]);
                        myRole = parts[2]; // "SHOOTER" or "GOALKEEPER"
                        handleTurnStart();
                    }
                    break;
                case "TURN_RESULT": // Server gửi: TURN_RESULT:<shooterZone>:<keeperZone>:<result>:<yourScore>:<opponentScore>:<shooterName>
                     if (parts.length >= 7) {
                        int shooterZone = Integer.parseInt(parts[1]);
                        int keeperZone = Integer.parseInt(parts[2]);
                        String result = parts[3]; // "GOAL" or "SAVE"
                        myScore = Integer.parseInt(parts[4]);
                        opponentScore = Integer.parseInt(parts[5]);
                        String shooterName = parts[6];
                        handleTurnResult(shooterZone, keeperZone, result.equals("GOAL"), shooterName);
                    }
                    break;
                case "MATCH_END": // Server gửi: MATCH_END:<winner_or_DRAW>:<yourFinalScore>:<opponentFinalScore>
                    if (parts.length >= 4) {
                        String winner = parts[1];
                        myScore = Integer.parseInt(parts[2]); // Cập nhật điểm cuối cùng
                        opponentScore = Integer.parseInt(parts[3]);
                        handleMatchEnd(winner);
                    }
                    break;
                 case "WAITING_FOR_OPPONENT":
                     handleWaiting();
                     break;
                 case "YOUR_TURN": // Server gửi: YOUR_TURN:<your_role> (thay thế cho TURN_START?)
                     if (parts.length >= 2) {
                         myRole = parts[1];
                         handleTurnStart(); // Bắt đầu lượt của mình
                     }
                     break;
                case "OPPONENT_DISCONNECTED":
                    handleOpponentDisconnected();
                    break;
                default:
                    System.out.println("MatchController received unknown command: " + message);
            }
        });
    }
    
    private void handleMatchStart(String firstShooter) {
        isMyTurn = firstShooter.equals(playerName);
        myRole = isMyTurn ? "SHOOTER" : "GOALKEEPER";

        matchView.updateOpponentName(opponentName); // Cập nhật tên đối thủ trên UI
        matchView.updateMessage("Match starts! " + opponentName + " vs " + playerName +
                ". " + firstShooter + " shoots first.");

        // Không cần delay ở client, chờ server gửi TURN_START hoặc YOUR_TURN
         if (!isMyTurn) {
             handleWaiting(); // Nếu không phải lượt mình thì chờ
         } else {
             // Nếu là lượt mình, chờ server gửi YOUR_TURN hoặc TURN_START
             matchView.updateMessage("Waiting for turn start...");
         }
    }
    
    private void handleTurnStart() {
         isMyTurn = true; // Đến lượt mình
         choiceConfirmed = false; // Reset trạng thái xác nhận
         selectedZone = -1; // Reset lựa chọn ô
         remainingSeconds = 15; // Reset thời gian đếm ngược

         matchView.resetField(); // Reset hiển thị sân
         matchView.enableChoosingZone(); // Cho phép chọn ô

         if (myRole.equals("SHOOTER")) {
             matchView.updateMessage("Your turn to SHOOT! Round " + currentRound + " (" + remainingSeconds + "s)");
         } else { // GOALKEEPER
             matchView.updateMessage("Your turn to SAVE! Round " + currentRound + " (" + remainingSeconds + "s)");
         }

         startTurnTimer(); // Bắt đầu đếm ngược
     }
    
    private void handleWaiting() {
         isMyTurn = false;
         matchView.disableInput(); // Không cho người dùng thao tác
         matchView.updateMessage("Waiting for " + opponentName + " to " + (myRole.equals("SHOOTER") ? "save..." : "shoot..."));
         stopTurnTimer(); // Dừng timer nếu có
     }
    
    private void startTurnTimer() {
        stopTurnTimer(); // Dừng timer cũ nếu đang chạy

        turnTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingSeconds--;

            if (remainingSeconds > 0) {
                 String action = myRole.equals("SHOOTER") ? "SHOOT" : "SAVE";
                 matchView.updateMessage("Your turn to " + action + "! Round " + currentRound + " (" + remainingSeconds + "s)");
                 // Cập nhật timer trên UI nếu có
                 matchView.updateTimer(remainingSeconds);
            } else {
                // Hết giờ - tự động gửi lựa chọn (random nếu chưa chọn)
                 stopTurnTimer();
                 if (!choiceConfirmed) {
                    onConfirmChoice(); // Gọi hàm xác nhận (sẽ random nếu chưa chọn)
                 }
            }
        }));
        turnTimer.setCycleCount(remainingSeconds); // Chạy số lần bằng số giây còn lại
        turnTimer.play();
        matchView.updateTimer(remainingSeconds); // Cập nhật UI lần đầu
    }
    
    private void stopTurnTimer() {
         if (turnTimer != null) {
             turnTimer.stop();
             turnTimer = null;
         }
         matchView.updateTimer(-1); // Ẩn timer hoặc hiển thị hết giờ
     }

    public void onZoneSelected(int zoneIndex) {
        if (!isMyTurn || choiceConfirmed) return; // Chỉ cho chọn khi đến lượt và chưa xác nhận

        selectedZone = zoneIndex;
        String[] zoneNames = {"Top Left", "Top Center", "Top Right",
                             "Bottom Left", "Bottom Center", "Bottom Right"};
        matchView.updateMessage("Selected: " + zoneNames[zoneIndex] + " - Confirm your choice!");
        matchView.highlightSelectedZone(zoneIndex); // Yêu cầu View highlight ô đã chọn
    }
    
    public void onConfirmChoice() {
        if (!isMyTurn || choiceConfirmed) return; // Chỉ xác nhận 1 lần mỗi lượt

        stopTurnTimer(); // Dừng đếm ngược
        choiceConfirmed = true; // Đánh dấu đã xác nhận
        matchView.disableInput(); // Khóa input lại

        // Nếu chưa chọn ô nào (ví dụ hết giờ), chọn random
        if (selectedZone == -1) {
            selectedZone = (int) (Math.random() * 6);
            System.out.println("No zone selected, choosing random: " + selectedZone);
        }

        // Gửi lựa chọn lên server
        // Format: CHOICE:<match_id>:<zone_number> (Cần có match_id, tạm bỏ qua nếu server tự biết)
        clientListener.sendMessage("CHOICE:" + selectedZone);

        // Chuyển sang trạng thái chờ đối thủ / chờ kết quả
         handleWaiting();
         matchView.updateMessage("Choice confirmed ("+selectedZone+"). Waiting for result...");
    }
    
    private void handleTurnResult(int shooterZone, int keeperZone, boolean isGoal, String shooterName) {
         updateViewScores(); // Cập nhật điểm trên UI trước

         boolean iWasShooter = shooterName.equals(playerName);

         // Hiển thị animation trên MatchView
         // Cần thêm các hàm này trong MatchView
         if (iWasShooter) {
             matchView.playShootAnimation(shooterZone, keeperZone, isGoal, result -> {
                 showTurnResultMessage(isGoal, true); // Hiển thị thông báo sau anim
                 // Chờ server gửi TURN_START hoặc MATCH_END cho lượt tiếp theo
                 matchView.updateMessage("Waiting for next turn...");
             });
         } else { // I was the keeper
             matchView.playGoalkeeperAnimation(shooterZone, keeperZone, isGoal, result -> {
                 showTurnResultMessage(isGoal, false); // Hiển thị thông báo sau anim
                 // Chờ server gửi TURN_START hoặc MATCH_END cho lượt tiếp theo
                 matchView.updateMessage("Waiting for next turn...");
             });
         }
         // Reset selected zone cho lượt sau
         selectedZone = -1;
     }
    
    private void showTurnResultMessage(boolean isGoal, boolean iWasShooter) {
        String resultMsg;
        if (iWasShooter) {
            resultMsg = isGoal ? "GOAL! You scored!" : "SAVED! Your shot was blocked!";
        } else {
            resultMsg = isGoal ? "GOAL! " + opponentName + " scored." : "SAVED! You blocked the shot!";
        }
         // Hiển thị kết quả tạm thời, sau đó chờ lượt tiếp theo
         matchView.updateMessage(resultMsg + " Score: " + myScore + "-" + opponentScore);
    }
    
    private void updateViewScores() {
        matchView.updateScore(myScore, opponentScore);
    }
    
    private void prepareNextTurn() {
        // Wait 2 seconds before next turn
        Timeline delay = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            matchView.resetField();
            // Server will send TURN_START for next turn
        }));
        delay.play();
    }

    // Hiển thị lỗi và đóng cửa sổ
    public void showErrorAndClose(String message) {
        matchView.showErrorMessage(message);
         // Có thể đóng cửa sổ ngay hoặc sau khi người dùng nhấn OK
        closeMatchView();
    }
    
    private void handleMatchEnd(String winner) {
        stopTurnTimer();
        isMyTurn = false;
        matchView.disableInput();
        updateViewScores(); // Cập nhật điểm cuối

        String resultMessage;
        if (winner.equals(playerName)) {
            resultMessage = "🎉 YOU WIN! 🎉\n" +
                          "Final Score: " + myScore + " - " + opponentScore;
        } else if (winner.equals("DRAW")) {
            // Trường hợp này ít xảy ra nếu có sudden death, nhưng cứ xử lý
            resultMessage = "🤝 IT'S A DRAW! 🤝\n" +
                          "Final Score: " + myScore + " - " + opponentScore;
        } else { // Đối thủ thắng
             resultMessage = "😢 You Lost! 😢\n" +
                           "Winner: " + opponentName + "\n" +
                           "Final Score: " + myScore + " - " + opponentScore;
        }

        matchView.showMatchEndMessage(resultMessage); // Hiển thị dialog kết quả
        // Có thể thêm nút "Play Again" hoặc "Back to Lobby" ở đây
        // Sau khi người dùng đóng dialog, có thể đóng cửa sổ trận đấu
        // closeMatchView(); // Hoặc chờ người dùng bấm nút nào đó
    }
    
    public void requestRematch() {
        clientListener.sendMessage("REMATCH_REQUEST");
    }
    
    private void handleOpponentDisconnected() {
        stopTurnTimer();
        isMyTurn = false;
        matchView.disableInput();
        showErrorAndClose("Opponent disconnected! You win by default.");
        // Đóng cửa sổ sau khi hiển thị lỗi
        // closeMatchView(); // Gọi sau khi người dùng nhấn OK trên dialog lỗi
    }
    
    public void onWindowClose() {
        stopTurnTimer();
         clientListener.sendMessage("LEAVE_MATCH");
//        try {
//            SocketService.close(); // Đóng socket khi thoát cửa sổ game
//        } catch (IOException e) { e.printStackTrace(); }
    }
}
