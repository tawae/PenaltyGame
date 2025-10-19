/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package penaltyclient.controller;

import penaltyclient.view.LobbyView;
import java.io.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import penaltyclient.model.ClientListener;
import penaltyclient.model.SocketService;
/**
 *
 * @author This PC
 */
public class LobbyController {

    /**
     * @param args the command line arguments
     */
    private LobbyView lobbyView;
    private LoginController loginController;
    private ObjectOutputStream out = SocketService.getOutputStream();
    private ObjectInputStream in = SocketService.getInputStream();

    private String username;

    public LobbyController(String username) {
        this.lobbyView = new LobbyView(username);
        this.lobbyView.setVisible(true);
        this.loginController = new LoginController();
        this.username = username;
        this.loadPlayers();
        
        new Thread(new ClientListener(this)).start();
    }
    public void showLobbyView() {
        this.lobbyView.setVisible(true);
    }
    public void hideLobbyView() {
        this.lobbyView.dispose();
    }

    public void loadPlayers() {
        

        try {   
            // gui yeu cau lay onlineusers
            out.writeObject("GET_ONLINE_USERS");
            out.flush();

            // nhan du lieu
            List<String> users = (List<String>)in.readObject();
            for(String user : users) {
                if(user.equals(username))
                    continue;
                this.lobbyView.addPlayer(user, "online", 0);
            }
        } catch (IOException ex) {
            Logger.getLogger(LobbyController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(LobbyController.class.getName()).log(Level.SEVERE, null, ex);
        }

        
    }

    public LobbyView getLobbyView() {
        return lobbyView;
    }

    public void setLobbyView(LobbyView lobbyView) {
        this.lobbyView = lobbyView;
    }
    
    
        
    public void handleLogout() {
        try {
            out.writeObject("LOGOUT");
            out.flush();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        
        this.hideLobbyView();
        loginController.showLoginView();
    }

    public void handleInvite(String playerName) {
        try {
            out.writeObject("INVITE:" + playerName);
            out.flush();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    
    
    
    
    
}