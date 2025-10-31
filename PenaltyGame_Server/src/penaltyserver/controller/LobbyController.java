/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package penaltyserver.controller;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import penaltyserver.model.ClientHandler;
import penaltyserver.model.Match;
import penaltyserver.model.MatchDAO;
import penaltyserver.model.MatchResult;
import penaltyserver.model.MatchResultDAO;
import penaltyserver.model.PenaltyShotDAO;
import penaltyserver.model.SessionManager;
import penaltyserver.model.User;
import penaltyserver.model.UserDAO;

/**
 *
 * @author This PC
 */
public class LobbyController {
    private static MatchController matchController;
    private static UserDAO userDAO;
    
    public static void setMatchController(MatchController matchController) {
        LobbyController.matchController = matchController;
    }
    
    public static void handleSendOnlineUsers(ObjectOutputStream out) throws IOException {
        List<String> onlineUsers = SessionManager.getOnlineUsers();
        out.writeObject(onlineUsers);
        out.flush();
        System.out.println("sent online list!");
        System.out.println(onlineUsers);
    }
    
    // A = nguoi moi, B = nguoi duoc moi
    public static void handleInviteB(String bUsername, ClientHandler selfHandler, String selfUsername) {
        ClientHandler bHandler = SessionManager.getSession(bUsername);

        // neu nguoi choi khong onl gui cho handler moi that bai
        if (bHandler == null) {
            bHandler.sendMessage("INVITE_FAIL:");
            System.out.println("Server sent to: " + selfHandler + " " + selfUsername + " fail invite from if sence" );
            return;
        }
        // Kiểm tra xem người B có đang trong trận không? (Cần thêm trạng thái vào User hoặc SessionManager)
        User userB = bHandler.getAssociatedUser();
        if (userB != null && userB.getCurrentMatchId() != null) {
            selfHandler.sendMessage("INVITE_FAIL:" + bUsername + " is currently in a match.");
            System.out.println("Server: Invite from " + selfUsername + " to " + bUsername + " failed (B in match).");
            return;
        }
        try {
            
            // gui thong bao moi cho b
            bHandler.sendMessage("INVITE_FROM:" + selfUsername);
            System.out.println("Server sent to: " + bHandler + " " + bUsername + " INVITE_FROM from try");
            // neu moi thanh cong tra thong bao cho a
            selfHandler.sendMessage("INVITE_SUCCESS:");
            System.out.println("Server sent to: " + selfHandler + " " + selfUsername + " INVITE_SUCCESS from try");
        }
        catch(Exception e) {
            // neu try loi tra fail ve cho a
            selfHandler.sendMessage("INVITE_FAIL:");
            System.out.println("Server sent to handler" + selfUsername + "Invite fail from catch ");
        }
    }
    // B la nguoi moi, sau khi phan hoi thi tra thong bao ve cho B
    public static void handleResponseInviteToB(String bUsername, User selfUser, boolean isAccept) {
        ClientHandler bHandler = SessionManager.getSession(bUsername);
//        ClientHandler selfHandler = SessionManager.getSession(selfUser.getUsername());
        if(bHandler == null) {
            System.out.println(bUsername + "is not online");
            return;
        }
        
        if(isAccept) {
            System.out.println(selfUser.getUsername() + " accepted invite from " + bUsername);
            bHandler.sendMessage("INVITE_RESPONSE_ACCEPT:" + selfUser.getUsername());
            
            User inviterUser = bHandler.getAssociatedUser();
//            MatchController.startMatch(bUsername, selfUser);
            if (inviterUser != null && selfUser != null) {
                // Kiểm tra lại xem cả 2 có còn available không (phòng trường hợp 1 người vào trận khác trong lúc chờ)
                if (inviterUser.getCurrentMatchId() == null && selfUser.getCurrentMatchId() == null) {
                    matchController.createAndStartMatch(inviterUser, selfUser);
                } else {
                    String busyPlayer = (inviterUser.getCurrentMatchId() != null) ? inviterUser.getUsername() : selfUser.getUsername();
                    bHandler.sendMessage("MATCH_FAIL:" + busyPlayer + " is already in a match.");
                    ClientHandler selfHandler = SessionManager.getSession(selfUser.getUsername());
                    if (selfHandler != null) {
                        selfHandler.sendMessage("MATCH_FAIL:" + busyPlayer + " is already in a match.");
                    }
                    System.err.println("Match creation failed: " + busyPlayer + " became busy.");
                }
            } else {
                System.err.println("Error creating match: User object missing for inviter or responder.");
                bHandler.sendMessage("MATCH_FAIL:Internal server error.");
            }
        }
        else {
            System.out.println(bUsername + "declined invite from " + selfUser.getUsername());
            bHandler.sendMessage("INVITE_RESPONSE_DECLINE:" + selfUser.getUsername());
        }
    }
    
}
    