package penaltyclient.controller;

import javafx.application.Platform;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import penaltyclient.view.MatchView;
import javafx.stage.Stage;
import penaltyclient.model.ClientListener;
//import network.ClientNetwork;

public class MatchController {
    private static MatchController currentlyStartingController = null;
    private MatchView matchView;
    private ClientListener clientListener;
    private LobbyController lobbyController;
    private Stage matchStage;

    private String playerName;
    private String opponentName;
    
    // Game state
    private boolean isMyTurn = false;
    private String myRole = ""; // "SHOOTER" or "GOALKEEPER"
    private int myScore = 0;
    private int opponentScore = 0;
    private int currentRound = 1;
    private int maxRounds = 5;
    
    // Turn timing
    private Timeline turnTimer;
    private int remainingSeconds = 10;
    private int selectedZone = -1;
    private boolean choiceConfirmed = false;
    
    public MatchController(String playerName, String opponentName, ClientListener clientListener, LobbyController lobbyController) {
//        this.matchView = view;
        this.clientListener = clientListener;
        this.playerName = playerName;
        this.opponentName = opponentName;
        this.lobbyController = lobbyController;
//        matchView.setController(this);
    }
    
    public void showMatchView() {
        // *** Quan trọng: Chạy trên JavaFX Thread ***
        Platform.runLater(() -> {
            try {
                // 1. Gán controller hiện tại vào biến static TRƯỚC KHI start View
                currentlyStartingController = this;

                // 2. Tạo Stage mới cho trận đấu
                matchStage = new Stage();

                // 3. Khởi tạo và chạy MatchView trên Stage mới
                // MatchView.start() sẽ tự động lấy controller từ biến static
                matchView = new MatchView(); // Khởi tạo instance View
                matchView.start(matchStage); // Gọi start() để View tự xây dựng UI

                // 4. Reset biến static sau khi View đã start và lấy controller
                currentlyStartingController = null;

                // Cập nhật thông tin ban đầu (sau khi View đã hiển thị)
                updateViewScores();
                matchView.updateOpponentName(opponentName);
                matchView.updateMessage("Waiting for match start signal...");

            } catch (Exception e) {
                e.printStackTrace();
                // Xử lý lỗi nghiêm trọng nếu không khởi tạo được View
                showErrorAndClose("Failed to launch match window.");
                // Reset biến static nếu lỗi
                currentlyStartingController = null;
            }
        });
    }
    
    public static MatchController getStartingController() {
        return currentlyStartingController;
    }

    // Gán instance MatchView thực tế sau khi nó được tạo
    // Hàm này có thể được gọi từ bên trong MatchView.start()
    public void registerViewInstance(MatchView viewInstance) {
        this.matchView = viewInstance;
        System.out.println("MatchController registered MatchView instance.");
    }
    
    private void closeMatchViewAndReturnToLobby() {
        Platform.runLater(() -> {
            if (matchStage != null && matchStage.isShowing()) {
                matchStage.close();
            }
            // Yêu cầu LobbyController hiển thị lại LobbyView
            if (lobbyController != null) {
                // Quan trọng: Báo cho ClientListener biết đã quay về lobby
                if (clientListener != null) {
                    clientListener.setLobbyController(lobbyController); // Chuyển listener về lobby
                }
                lobbyController.showLobbyView();
            } else {
                System.err.println("Error: LobbyController is null. Cannot return to lobby.");
                // Có thể quay về Login hoặc đóng ứng dụng
            }
            // Đảm bảo MatchController này không còn hoạt động
            if (clientListener != null) {
                clientListener.clearMatchController(); // Thêm hàm này vào ClientListener
            }
        });
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
             matchView.playShootAnimation(shooterZone, keeperZone, isGoal, () -> {
                 showTurnResultMessage(isGoal, true); // Hiển thị thông báo sau anim
                 // Chờ server gửi TURN_START hoặc MATCH_END cho lượt tiếp theo
                 matchView.updateMessage("Waiting for next turn...");
             });
         } else { // I was the keeper
             matchView.playGoalkeeperAnimation(shooterZone, keeperZone, isGoal, () -> {
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
        // Hiển thị lỗi trên cửa sổ match (nếu còn) hoặc cửa sổ mới
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Match Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            if (matchStage != null && matchStage.isShowing()) {
                alert.initOwner(matchStage);
            }
            alert.showAndWait();
            // Sau khi hiển thị lỗi, quay về Lobby
            closeMatchViewAndReturnToLobby();
        });
    }
    
    private void handleMatchEnd(String winner) {
        stopTurnTimer();
        isMyTurn = false;
        if (matchView != null) { // Kiểm tra null
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
            matchView.showMatchEndMessage(resultMessage, () -> {
                closeMatchViewAndReturnToLobby(); // Callback để quay về lobby sau khi đóng dialog
            });
        } else {
            // Xử lý trường hợp view chưa sẵn sàng hoặc đã bị lỗi
            closeMatchViewAndReturnToLobby();
        }
    }
    
    public void requestRematch() {
        clientListener.sendMessage("REMATCH_REQUEST");
    }
    
    private void handleOpponentDisconnected() {
        stopTurnTimer();
        isMyTurn = false;
        if (matchView != null) { // Kiểm tra null
            matchView.disableInput();
            // Hiển thị lỗi, sau đó quay về lobby
            matchView.showErrorMessage("Opponent disconnected! You win by default.", () -> {
                closeMatchViewAndReturnToLobby(); // Callback để quay về lobby
            });
        } else {
            // Xử lý trường hợp view chưa sẵn sàng hoặc đã bị lỗi
            closeMatchViewAndReturnToLobby();
        }
    }
    
    public void onWindowClose() {
        stopTurnTimer();
         clientListener.sendMessage("LEAVE_MATCH");
//        try {
//            SocketService.close(); // Đóng socket khi thoát cửa sổ game
//        } catch (IOException e) { e.printStackTrace(); }
    }
}
