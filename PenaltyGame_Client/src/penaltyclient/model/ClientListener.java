/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package penaltyclient.model;

import javax.swing.JOptionPane;
import java.io.*;
import penaltyclient.controller.LobbyController;
import penaltyclient.controller.MatchController;
import penaltyclient.view.LobbyView;
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
    
    @Override
    public void run() {
        try {
            while(true) {
                Object obj = in.readObject();
                if(obj instanceof String) {
                    String[] parts = ((String) obj).split(":");
                    String command = parts[0];

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

                        case "INVITE_RESPONSE_ACCEPT":
                            String responder = parts[1];
                            JOptionPane.showMessageDialog(lobbyView, "accepted by "  + responder);
                            break;

                        case "INVITE_RESPONSE_DECLINE":
                            String responder2 = parts[1];
                            JOptionPane.showMessageDialog(lobbyView, "declined by "  + responder2);
                            break;
                        case "START_MATCH":
                            int matchId = Integer.parseInt(parts[1]);
                            matchController = new MatchController(matchId, lobbyController.getUsername());
                            lobbyController.hideLobbyView();
                    }
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }


    public void sendMessage(String msg) {
        try {
            out.writeObject(msg);
            out.flush();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
