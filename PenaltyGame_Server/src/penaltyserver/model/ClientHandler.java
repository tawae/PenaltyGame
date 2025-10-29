/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package penaltyserver.model;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import penaltyserver.controller.AuthController;
import penaltyserver.controller.LobbyController;
import penaltyserver.controller.MatchController;

/**
 *
 * @author This PC
 */
public class ClientHandler extends Thread {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private User user;
    private String username;
    
    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush(); // flush header ngay
            this.in = new ObjectInputStream(socket.getInputStream());

        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void run() {
        try {
            while(true) {
                Object obj = in.readObject();
                if(obj instanceof String) {
                    String msg = (String) obj;
                    
                    // split with form a:b a is command, b is data and can have many data like a:b:c
                    String[] parts = msg.split(":");
                    String command = parts[0];
                    
                    switch(command) {
                        case "LOGIN": {
                            this.username = parts[1];
                            String password = parts[2];
                            user = new User(username, password);
            
                            if (AuthController.checkLogin(user)) {
                                SessionManager.addSession(username, this);
                                out.writeObject("LOGIN_SUCCESS");
                                out.flush();
                            } else {
                                out.writeObject("LOGIN_FAIL");
                                out.flush();
                                return;
                            }
                            break;

                        }                     
                        case "LOGOUT": {
                            SessionManager.removeSession(username);
                            System.out.println(username + "dang xuat");
                            break;
                        }
                        case "GET_ONLINE_USERS":
                            LobbyController.handleSendOnlineUsers(out);
                            System.out.println(username + "đang online");
                            break;
                        case "INVITE":
                            String targetUsername = parts[1];
                            LobbyController.handleInviteB(targetUsername, this, username);
                            System.out.println(username + "mời" + targetUsername);
                            break;
                            
                        case "INVITE_ACCEPT":
                            String fromUser = parts[1];
                            LobbyController.handleResponseInviteToB(fromUser, user, true);
                            break;
                            
                            
                        case "INVITE_DECLINE":
                            String fromUser2 = parts[1];
                            LobbyController.handleResponseInviteToB(fromUser2, user, false);
                            break;
                        
                        default: System.out.println("Unknown command:" + msg);
                    }
                }
            }
        }catch (EOFException | SocketException e) {
            // client đóng kết nối
            System.out.println("Client " + username + " disconnected.");
            SessionManager.removeSession(username);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
    }
    public void sendMessage(String msg) {
        try {
            this.out.writeObject(msg);
            this.out.flush();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }   
}
