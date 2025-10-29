/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package penaltyclient;

import javafx.application.Application;
import javafx.stage.Stage;
import penaltyclient.controller.LoginController;
import java.io.IOException;


public class PenaltyClient extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Khởi tạo LoginController và truyền vào primaryStage
        LoginController loginController = new LoginController(primaryStage);
        loginController.showLoginView();
    }
    
    public static void main(String[] args) {
        System.out.println("--- CHUONG TRINH BAT DAU (HAM MAIN) ---");
        // Phương thức launch() sẽ gọi start()
        launch(args);
    }
}
