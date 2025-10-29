package penaltyclient.controller;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage; // Cần import Stage
import penaltyclient.view.LoginView;
import penaltyclient.model.SocketService;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import penaltyclient.controller.LobbyController;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginController {

    private LoginView loginView;
    private Stage stage; // 1. Thêm biến để lưu Stage (cửa sổ)

    public LoginController(Stage stage) {
        this.stage = stage;
        // Việc khởi tạo LoginView được chuyển vào hàm showLoginView()
    }

    public void showLoginView() {
        // 3. Khởi tạo LoginView ở đây
        this.loginView = new LoginView(this);
        
        // Tạo một Scene (cảnh) mới chứa giao diện của LoginView
        Scene scene = new Scene(loginView.getView(), 350, 220);
        // 4. Set Scene này cho Stage
        stage.setTitle("Login");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public void hideLoginView() {
        stage.hide(); // Dùng stage để ẩn cửa sổ
    }
    
    public void login(String username, String password) {
        if(username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Username and password must not be empty.");
            return;
        }

        try {
            SocketService.connect("localhost", 12345);
            
            ObjectOutputStream out = SocketService.getOutputStream();
            ObjectInputStream in = SocketService.getInputStream();
                    
            
            out.writeObject("LOGIN:" + username + ":" + password);
            out.flush(); // Thêm flush để đảm bảo dữ liệu được gửi đi ngay

        // response
            String response = (String) in.readObject();
            if(response.equals("LOGIN_SUCCESS")) {
                // Sử dụng Alert của JavaFX thay vì JOptionPane
                showAlert(Alert.AlertType.INFORMATION, "Login Success");
                
                // Khi login thành công, ta gọi LobbyController
                // và truyền Stage qua cho nó
                new LobbyController(username, stage, this).showLobbyView();

            }
            else {
                showAlert(Alert.AlertType.ERROR, "Invalid information");
            }
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(LoginController.class.getName()).log(Level.SEVERE, null, ex);
            showAlert(Alert.AlertType.ERROR, "Could not connect to server: " + ex.getMessage());
        }   
    }

    private void showAlert(Alert.AlertType alertType, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle("Login Status");
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // === SỬA LỖI: THÊM DÒNG NÀY ===
        // Đặt cửa sổ chính (stage) làm chủ của Alert
        alert.initOwner(stage);
        // ============================
        
        alert.showAndWait();
    }
}