/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package penaltyserver.model;


import java.util.*;
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
/**
 *
 * @author This PC
 */
public class SessionManager {
    private static Map<String, ClientHandler> sessions = new ConcurrentHashMap<>();

    public static void addSession(String username, ClientHandler handler) {
        sessions.put(username, handler);
    }
    public static void removeSession(String username) {
        sessions.remove(username);
    }
    
    public static ClientHandler getSession(String username) {
        return sessions.get(username);
    }
    
    public static List<String> getOnlineUsers() {
        return new ArrayList<>(sessions.keySet());
    }
    public static boolean isOnline(String username) {
        return sessions.containsKey(username);
    }
}