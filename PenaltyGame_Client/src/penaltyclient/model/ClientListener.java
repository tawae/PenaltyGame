package penaltyclient.model;

import javax.swing.JOptionPane;
import java.io.*;
import java.net.SocketException;
import penaltyclient.controller.LobbyController;
import penaltyclient.controller.MatchController;
import penaltyclient.view.LobbyView;
import javafx.application.Platform;
import java.io.ObjectInputStream;
import javafx.application.Platform; 
import penaltyclient.controller.LobbyController;
import java.io.IOException;
import java.util.List; // Cần import List
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.stage.Stage;

/**
 *
 * @author This PC
 */

public class ClientListener implements Runnable {
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private LobbyController lobbyController;
    private MatchController matchController;
    private LobbyView lobbyView;
    private Stage stage;
    
    public ClientListener(LobbyController lobbyController, Stage stage) {
        this.stage = stage;
        this.lobbyController = lobbyController;
        try {
            out = SocketService.getOutputStream();
            in = SocketService.getInputStream();
        } catch (IOException e) {
            Logger.getLogger(ClientListener.class.getName()).log(Level.SEVERE, null, e);
        }
    }
    
    public void setLobbyController(LobbyController lobbyController) {
        this.lobbyController = lobbyController;
        this.matchController = null; // Không còn ở trong trận đấu
        System.out.println("ClientListener context switched back to Lobby.");
    }

    // Hàm để xóa tham chiếu MatchController (khi quay về lobby)
    public void clearMatchController() {
        this.matchController = null;
    }
    
    public void setMatchController(MatchController matchController) {
        this.matchController = matchController;
        this.lobbyController = null; // Không cần lobby nữa khi vào trận
        this.lobbyView = null;
    }
    
    @Override
    public void run() {
        try {
            while(true) {
                Object obj = in.readObject(); 
                
                if (obj != null) {
                    System.out.println("Listener RECEIVED object of type: " + obj.getClass().getName());
                } else {
                    System.out.println("Listener RECEIVED null object!"); // Should not happen often
                    continue;
                }
                if (obj instanceof String) {
                    // Xử lý các command dạng String
                    String message = (String) obj;
                    System.out.println("REceived from server: " + message);
                    String[] parts = message.split(":");
                    String command = parts[0];

                    if(matchController != null) {
                        // Đang trong trận -> Gửi cho MatchController
                        final String finalMsg = message; // Biến final để dùng trong lambda
                        Platform.runLater(() -> {
                            // Kiểm tra lại matchController trước khi gọi (phòng trường hợp vừa quay về lobby)
                            if (matchController != null) {
                                matchController.handleServerMessage(finalMsg);
                            }
                        });
                    }else if (lobbyController != null) {
                        switch(command) {
                            case "INVITE_FROM": {
                                String invitePlayer = parts[1];
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        lobbyController.showInvitationAlert(invitePlayer);
                                    }
                                });
                                break;
                            }
                            case "INVITE_SUCCESS": {
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        lobbyController.showAlert("Invitation Sent", "Invited successfully!");
                                    }
                                });
                                break;
                            }

                            case "INVITE_FAIL":
                            case "INVITE_RESPONSE_ACCEPT":
                            case "INVITE_RESPONSE_DECLINE":
                                final String alertTitle = command;
                                final String alertMessage = parts[1];
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (alertTitle == "INVITE_RESPONSE_ACCEPT") {
                                            lobbyController.showAlert("Accepted", alertMessage + " accepted!");
                                        } else if (alertTitle == "INVITE_RESPONSE_DECLINE") {
                                            lobbyController.showAlert("Declined", alertMessage + " declined!");
                                        } else {
                                            lobbyController.showAlert(alertTitle, alertMessage);
                                        }
                                    }
                                });
                                break;

                            case "MATCH_START": {
                                if (parts.length >= 3) {
                                    String opponentUsername = parts[1];
                                    String firstShooter = parts[2]; // Ai sút trước

                                    // **QUAN TRỌNG: Chuyển đổi View trên JavaFX Thread**
                                    Platform.runLater(() -> {
                                        System.out.println("Client: Received START_MATCH, attempting to switch view...");
                                        // 1. Tạo MatchController MỚI
                                        // Cần truyền Stage hiện tại (mainStage) vào MatchController
                                        // Hoặc để MatchController tạo Stage mới và ẩn Stage cũ
                                        lobbyController.hideLobbyView();
                                        matchController = new MatchController(
                                                lobbyController.getUsername(),
                                                opponentUsername,
                                                this, // Truyền chính ClientListener này
                                                lobbyController
                                        );

                                        this.matchController = matchController;
                                        this.lobbyController = null;
                                        // 2. Chuyển quyền quản lý sang MatchController
                                        setMatchController(matchController);

                                        // 3. Yêu cầu MatchController hiển thị MatchView
                                        // (Hàm này nên xử lý việc thay Scene trên mainStage)
                                        matchController.showMatchView();// Đổi tên hàm thành showMatchScene

                                        // 4. Gửi thông tin bắt đầu cho MatchController (sau khi view sẵn sàng)
                                        // Chuyển việc gọi handleMatchStart vào trong showMatchScene hoặc sau đó một chút
                                        // matchController.handleMatchStart(firstShooter); // Gọi sau khi Scene đã hiển thị
                                    });
                                } else {
                                    System.err.println("Client: Invalid START_MATCH message format: " + message);
                                }
                                break;
                            }
                        }
                    }
                } else if (obj instanceof List) { 
                        try {
                            // Giả định đây là List<String>
                            @SuppressWarnings("unchecked") // Bỏ qua cảnh báo cast
                            List<String> userList = (List<String>) obj;

                            // Gọi hàm cập nhật giao diện trong LobbyController
                            lobbyController.updateOnlinePlayers(userList);

                        } catch (ClassCastException e) {
                            System.err.println("ClientListener: Đã nhận 1 List nhưng không phải List<String>!");
                        }
                        // TODO: Bạn cũng nên làm điều tương tự cho
                        // List<MatchRecord> (cho lịch sử) và List<RankingEntry> (cho xếp hạng)
                        // Bằng cách kiểm tra `obj instanceof List` và kiểm tra phần tử đầu tiên
                    }
                }
            } catch (EOFException | SocketException e) {
             System.out.println("Connection closed by server or client.");
             // Có thể thêm logic để hiển thị thông báo lỗi và quay về màn hình Login
             Platform.runLater(() -> {
                if(matchController != null) matchController.showErrorAndClose("Connection lost.");
                // Cần thêm xử lý quay về màn hình Login
             });
        } catch (Exception e) {
            e.printStackTrace();
             Platform.runLater(() -> {
                if(matchController != null) matchController.showErrorAndClose("An error occurred: " + e.getMessage());
                 // Cần thêm xử lý quay về màn hình Login
             });
        } finally {
             // Dọn dẹp tài nguyên nếu cần
             // try { SocketService.close(); } catch (IOException ioEx) { }
        }
    }

    private void handleConnectionLoss() {
        Platform.runLater(() -> {
            if (matchController != null) {
                matchController.showErrorAndClose("Connection lost.");
                matchController = null; // Reset controller
            } else if (lobbyController != null) {
                lobbyController.showAlert("Error", "Connection lost. Returning to login screen.");
                // Logic quay về màn hình Login
                lobbyController.handleLogout(); // Thêm hàm này để quay về Login
                lobbyController = null; // Reset controller
            }
            // Có thể đóng SocketService ở đây nếu chắc chắn không kết nối lại
            // try { SocketService.close(); } catch (IOException ioEx) {}
        });
    }

    public void sendMessage(String msg) {
        try {
            out.writeObject(msg);
            out.flush();
            System.out.println("client send: " + msg);
        }
        catch(Exception e) {
            System.out.println("ClientListener ngắt kết nối: " + e.getMessage());
            // (Lỗi StreamCorruptedException sẽ xảy ra ở đây nếu 2 luồng cùng đọc)
            e.printStackTrace();
        }
    }
}