/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package penaltyclient.controller;

import penaltyclient.view.LoginView;
import penaltyclient.view.LobbyView;
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 *
 * @author This PC
 */
public class LoginController {
    private LoginView loginView;

    public LoginController(LoginView loginView) {
        this.loginView = loginView;

        this.loginView.addLoginListener(e -> login());
        System.out.println("Login button clicked!");
        //loginView.btnExit.addActionListener(e -> System.exit(0));
    }

    /**
     * @param args the command line arguments
     */
    private void login() {
        String user = loginView.txtUsername.getText();
        String pass = new String(loginView.txtPassword.getPassword());
        System.out.println(user + " " + pass);
        
        try(Socket socket = new Socket("localhost", 12345)) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            // send to server
            out.writeObject(user);
            out.writeObject(pass);
            System.out.println("sending " + user + " " + pass + " to server...");

            // response
            String response = (String) in.readObject();
            System.out.println("received " + response + " from server!");
            if(response.equals("SUCCESS")) {
                JOptionPane.showMessageDialog(loginView, "Login Success");
                
                loginView.dispose();
                
                LobbyView lobbyView = new LobbyView(user);
                lobbyView.setVisible(true);
            }
            else {
                JOptionPane.showMessageDialog(loginView, "Invalid information");
            }
        }
        catch(Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(loginView, "Error connecting to server!");
        }
    }
}
