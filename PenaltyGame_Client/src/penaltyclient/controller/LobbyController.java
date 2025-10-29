package penaltyclient.controller;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert; 
import javafx.scene.control.ButtonType; 
import javafx.stage.Stage; // Import
import penaltyclient.view.LobbyView;
import java.io.*;
import java.util.List;
import java.util.Optional; 
import java.util.logging.Level;
import java.util.logging.Logger;
import penaltyclient.model.ClientListener;
import penaltyclient.model.SocketService;
import penaltyclient.controller.MatchController;
import javafx.application.Platform;

import penaltyclient.model.MatchRecord;
import penaltyclient.model.RankingEntry;
import java.util.ArrayList;

/**
 *
 * @author This PC
 */
public class LobbyController {

    private LobbyView lobbyView;
    private LoginController loginController;

    private Stage stage; // Biến này lưu cửa sổ chính
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private ClientListener clientListener;

    private String username;

    public LobbyController(String username, Stage stage, LoginController loginController) {
        this.username = username;
        this.stage = stage;
        this.loginController = loginController;
        this.lobbyView = new LobbyView(username, this);

        try {
            this.out = SocketService.getOutputStream();
            this.in = SocketService.getInputStream();
            if (this.clientListener == null) { // Chỉ tạo nếu chưa có (an toàn hơn)
                this.clientListener = new ClientListener(this, this.stage);
                new Thread(this.clientListener).start(); // Chạy listener trên luồng riêng
                System.out.println("ClientListener started for Lobby.");
            }
        } catch (IOException e) {
            Logger.getLogger(LobbyController.class.getName()).log(Level.SEVERE, null, e);
            handleLogout();
        }
    }

    public void showLobbyView() {
        Scene scene = new Scene(lobbyView.getView(), 600, 450); 
        stage.setTitle("Lobby - " + username);
        stage.setScene(scene);
        stage.setResizable(true); 
        stage.show();

        if (this.clientListener == null) {
            this.clientListener = new ClientListener(this, stage);
            new Thread(this.clientListener).start();
            System.out.println("clientlistener ready");
        } else {
            clientListener.setLobbyController(this);
            System.out.println("client listener reset to lobby");
        }
        
        if(!stage.isShowing()) {
            stage.show();
        }
        
        this.loadPlayers();
    }

    public void loadPlayers() {
        System.out.println("loadPlayer running");
        try {   
            sendMessage("GET_ONLINE_USERS"); 
        } catch (Exception ex) {
            Logger.getLogger(LobbyController.class.getName()).log(Level.SEVERE, "Lỗi khi gửi yêu cầu loadPlayers", ex);
        }
    }
    
    public void updateOnlinePlayers(List<String> users) {
        Platform.runLater(new Runnable() { 
            @Override
            public void run() {
                lobbyView.clearOnlinePlayers(); 
                if (users != null) {
                    for (String user : users) {
                        if (user.equals(username))
                            continue;
                        lobbyView.addPlayer(user, "online", 0);
                    }
                }
            }
        });
    }

    public void handleReloadPlayers() {
        System.out.println("Reloading players (gửi yêu cầu)...");
        loadPlayers();
    }
    
    public void loadMatchHistory() {
        System.out.println("Loading match history...");
        // TODO: sendMessage("GET_MATCH_HISTORY");
        
        List<MatchRecord> records = new ArrayList<>();
        records.add(new MatchRecord("PlayerB", "Win (5-3)", "2025-10-28 10:30"));
        records.add(new MatchRecord("PlayerC", "Loss (1-4)", "2025-10-27 09:15"));
        lobbyView.updateMatchHistory(records);
    }

    public void loadRanking() {
        System.out.println("Loading ranking...");
        // TODO: sendMessage("GET_RANKING");
        
        List<RankingEntry> entries = new ArrayList<>();
        entries.add(new RankingEntry(1, "BestPlayer", 1500, 50));
        entries.add(new RankingEntry(2, username, 1450, 48)); 
        entries.add(new RankingEntry(3, "PlayerC", 1400, 45));
        lobbyView.updateRanking(entries);
    }
    
    public LobbyView getLobbyView() {
        return lobbyView;
    }
    
    public void handleLogout() {
        try {
            sendMessage("LOGOUT"); 
            SocketService.close(); 
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        loginController.showLoginView();
    }

    public void handleInvite(String playerName) {
        sendMessage("INVITE:" + playerName); 
    }

    public void sendMessage(String msg) {
        System.out.println("Sending msg: " + msg);
        try {
            if (out != null) {
                out.writeObject(msg);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Connection Error", "Could not send message to server.");
        }
    }

    public void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        alert.initOwner(stage);
        
        alert.showAndWait();
    }

    public void showInvitationAlert(String inviter) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Game Invitation");
        alert.setHeaderText(inviter + " muốn chơi với bạn!");
        alert.setContentText("Bạn có đồng ý không?");

        ButtonType btnYes = new ButtonType("Accept");
        ButtonType btnNo = new ButtonType("Refuse");
        alert.getButtonTypes().setAll(btnYes, btnNo);

        alert.initOwner(stage);

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == btnYes) {
            sendMessage("INVITE_ACCEPT:" + inviter);
        } else {
            sendMessage("INVITE_DECLINE:" + inviter);
        }
    }

    public void startMatch(int matchId) {
        showAlert("Match Starting", "Trận đấu " + matchId + " đang bắt đầu!");
        // TODO: Chuyển sang MatchController (JavaFX)
    }

    public String getUsername() {
        return username;
    }
}