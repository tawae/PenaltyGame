/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package penaltyclient.model;

import javax.swing.JOptionPane;
import java.io.*;
import java.net.SocketException;
import penaltyclient.controller.LobbyController;
import penaltyclient.controller.MatchController;
import penaltyclient.view.LobbyView;
import javafx.application.Platform;
/**
 *
 * @author This PC
 */
public class ClientListener implements Runnable {
    private ObjectOutputStream out = SocketService.getOutputStream();
    private ObjectInputStream in = SocketService.getInputStream();
    private LobbyController lobbyController;
    private LobbyView lobbyView;
    private MatchController matchController;
    
    public ClientListener(LobbyController lobbyController) {
        this.lobbyController = lobbyController;
        this.lobbyView = this.lobbyController.getLobbyView();
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
                if(obj instanceof String) {
                    String msg = (String) obj;
                    String[] parts = msg.split(":");
                    String command = parts[0];

                    if (matchController != null) {
                        Platform.runLater(() -> matchController.handleServerMessage(msg));
                    } else if (lobbyController != null) {
                        switch(command) {
                            case "INVITE_FROM": {
                                String invitePlayer = parts[1];
                                String[] options = {"Accept", "Refuse"};

                                int choice = JOptionPane.showOptionDialog(
                                        lobbyView, // giao dien hien thi
                                        "You have been invited by " + invitePlayer, //message
                                        "Lời mời",                     // tiêu đề dialog
                                        JOptionPane.DEFAULT_OPTION,    // kiểu option
                                        JOptionPane.INFORMATION_MESSAGE, // icon
                                        null,                          // icon custom
                                        options,                       // text của các nút
                                        options[0]   
                                        );

                                // xử lý theo lựa chọn
                                if (choice == 0) {
                                    // người chơi bấm Đồng ý
                                    sendMessage("INVITE_ACCEPT:" + invitePlayer);
                                } else if (choice == 1) {
                                    // người chơi bấm Từ chối
                                    sendMessage("INVITE_DECLINE:" + invitePlayer);
                                }
                                break;
                            }
                            case "INVITE_SUCCESS": {
                                JOptionPane.showMessageDialog(lobbyView, "Invited");
                                break;
                            }
                            case "INVITE_FAIL": {
                                JOptionPane.showMessageDialog(lobbyView, "Invite failed");
                                break;
                            }

                            case "INVITE_RESPONSE_ACCEPT": {
                                String responder = parts[1];
                                JOptionPane.showMessageDialog(lobbyView, "accepted by "  + responder);
                                break;
                            }

                            case "INVITE_RESPONSE_DECLINE": {
                                String responder2 = parts[1];
                                JOptionPane.showMessageDialog(lobbyView, "declined by "  + responder2);
                                break;
                            }
                            
                            case "START_MATCH": {
                                int matchId = Integer.parseInt(parts[1]);
                                String opponentUsername = parts[1];
                                // String firstShooter = parts[2]; // Và người sút trước

                                final String finalOpponent = opponentUsername;
                                // final String finalFirstShooter = firstShooter; // Nếu có

                                Platform.runLater(() -> {
                                    lobbyController.hideLobbyView(); // Ẩn lobby
                                    // Tạo MatchController, nó sẽ tự tạo MatchView
                                    matchController = new MatchController(lobbyController.getUsername(), finalOpponent, this);
                                    setMatchController(matchController); // Cập nhật listener để biết đang ở trong match
                                    matchController.showMatchView();
                                    // Thông báo cho MatchController biết trận đấu bắt đầu (có thể gộp vào constructor)
                                     matchController.handleMatchStart(new String[]{"MATCH_START", finalOpponent, finalFirstShooter});
                                    // Server cần gửi thêm message MATCH_START sau khi client sẵn sàng
                                });
                                break;
                            }
                            default:
                                System.out.println("unknown msg:" + msg);
                        }
                    }else {
                         System.out.println("Listener received message but no active controller: " + msg);
                    }
                }
            }
        } catch (EOFException | SocketException e) {
             System.out.println("Connection closed by server or client.");
             // Có thể thêm logic để hiển thị thông báo lỗi và quay về màn hình Login
             Platform.runLater(() -> {
                if(matchController != null) matchController.showErrorAndClose("Connection lost.");
                // Cần thêm xử lý quay về màn hình Login
             });
        }
        catch (Exception e) {
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


    public void sendMessage(String msg) {
        try {
            out.writeObject(msg);
            out.flush();
            System.out.println("client send: " + msg);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}