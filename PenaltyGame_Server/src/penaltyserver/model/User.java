/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package penaltyserver.model;
import java.io.*;
import java.net.*;

public class User {
//    private Socket socket;
    private int choice = -1;
    private int userId;
    private String username;
    private String password;
    private String currentMatchId = null;
    
    public int getUserId() {
        return userId;
    }
    public void setUserId(int userId) {
        this.userId = userId;
    }
    public String getUsername() {
        return this.username;
    }
    public String getPassword() {
        return this.password;
    }
    public void setUsername(String newUsername) {
        this.username = newUsername;
    }
    public void setPassword(String newPassword) {
        this.password = newPassword;
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        //this.socket = socket;
    }

//    public void setChoice(int choice) { 
//        this.choice = choice; 
//    }
//    public int getChoice() { 
//        return choice;
//    }

     // Override equals và hashCode để so sánh User dễ dàng hơn
//     @Override
//     public boolean equals(Object o) {
//         if (this == o) return true;
//         if (o == null || getClass() != o.getClass()) return false;
//         User user = (User) o;
//         return userId == user.userId && Objects.equals(username, user.username);
//     }
//
//     @Override
//     public int hashCode() {
//         return Objects.hash(userId, username);
//     }

//    public PrintWriter getWriter() throws IOException {
//        return new PrintWriter(socket.getOutputStream(), true);
//    }
    public void setCurrentMatchId (String matchId) {
        this.currentMatchId = matchId;
    }
    public String getCurrentMatchId() {
        return currentMatchId;
    }
}
