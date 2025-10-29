/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package penaltyclient.controller;

import penaltyclient.view.LoginView;
import penaltyclient.model.SocketService;
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author This PC
 */
public class LoginController {
    private LoginView loginView;

    public LoginController() {
        this.loginView = new LoginView(this);
    }

    public void showLoginView() {
        this.loginView.setVisible(true);
    }

    public void hideLoginView() {
        this.loginView.dispose();
    }
    
    public void login(String username, String password) {

        try {
            SocketService.connect("localhost", 12345);
            
            ObjectOutputStream out = SocketService.getOutputStream();
            ObjectInputStream in = SocketService.getInputStream();
                    
            
            out.writeObject("LOGIN:" + username + ":" + password);

        // response
            String response = (String) in.readObject();
            if(response.equals("LOGIN_SUCCESS")) {
                JOptionPane.showMessageDialog(loginView, "Login Success");
                loginView.dispose();
                new LobbyController(username);
            }
            else {
                JOptionPane.showMessageDialog(loginView, "Invalid information");
            }
        } catch (IOException ex) {
            Logger.getLogger(LoginController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(LoginController.class.getName()).log(Level.SEVERE, null, ex);
        }   
    }
}
