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
    
    private static UserDAO userDAO;
    
    public static void handleSendOnlineUsers(ObjectOutputStream out) throws IOException {
        List<String> onlineUsers = SessionManager.getOnlineUsers();
        out.writeObject(onlineUsers);
        out.flush();

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
        ClientHandler selfHandler = SessionManager.getSession(selfUser.getUsername());
        if(bHandler == null) return;
        
        if(isAccept) {
            bHandler.sendMessage("INVITE_RESPONSE_ACCEPT:" + selfUser.getUsername());
            
            MatchController.startMatch(bUsername, selfUser);
        }
        else {
            bHandler.sendMessage("INVITE_RESPONSE_DECLINE:" + selfUser.getUsername());
        }
    }
    
}
    