/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package penaltyserver;

import java.io.*;
import java.net.*;
import java.sql.*;
import penaltyserver.config.DBConnection;
/**
 *
 * @author This PC
 */
public class PenaltyServer {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try(ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("server is running ... ");
            
            while(true) {
                Socket socket = serverSocket.accept();
                System.out.println("Có 1 client đang kết nối...");

                new Thread(() -> handleClient(socket)).start();
            }

        }catch(IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket) {
        try (
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())
        ) {
            String username = (String) in.readObject();
            String password = (String) in.readObject();
            
            System.out.println("Thong tin nguoi dung da nhap: Username: " + username + ", Password: " + password);

            boolean checkLogin = checkLogin(username, password);

            out.writeObject(checkLogin ? "SUCCESS" : "FAILED");
            
        }
        catch(Exception e) {
            e.printStackTrace();
        }

    }

    private static boolean checkLogin(String username, String password) {
        try(Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
        catch(SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private int receiveChoice(DataInputStream in, String playerId) throws IOException {
        int choice = in.readInt();
        System.out.println("Player " + playerId + " chose: " + choice);
        return choice;
    }

    private void sendToPlayer(DataOutputStream out, String playerId, String msg) throws IOException {
        out.writeUTF(msg);
        out.flush();
        System.out.println("Sent to Player " + playerId + ": " + msg);
    }

    private void handlePenaltyRound(Socket shooter, Socket keeper, String shooterId, String keeperId) throws IOException {
        DataInputStream shooterIn = new DataInputStream(shooter.getInputStream());
        DataOutputStream shooterOut = new DataOutputStream(shooter.getOutputStream());

        DataInputStream keeperIn = new DataInputStream(keeper.getInputStream());
        DataOutputStream keeperOut = new DataOutputStream(keeper.getOutputStream());

        // Nhận lựa chọn
        int shooterChoice = receiveChoice(shooterIn, shooterId);
        int keeperChoice = receiveChoice(keeperIn, keeperId);

        // Xử lý kết quả
        String result = checkResult(shooterChoice, keeperChoice);

        // Trả kết quả cho cả 2
        sendToPlayer(keeperOut, keeperId, result);
        sendToPlayer(shooterOut, shooterId, result);
    }

    public static String checkResult(int shooterChoice, int keeperChoice) {
        // shooterChoice và keeperChoice là số từ 1-6 (ứng với 6 ô)
        if (shooterChoice == keeperChoice) {
            return "SAVED";   // Thủ môn bắt được
        } else {
            return "GOAL";   // Ghi bàn
        }
    }
    private static class ClientHandler implements Runnable {
        private Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                while (true) {
                    Object obj = in.readObject();
                    System.out.println("Received from client: " + obj);

                    // Xử lý logic game, ví dụ echo lại
                    out.writeObject("Server received: " + obj);
                    out.flush();
                }
            } catch (Exception e) {
                System.out.println("Client disconnected: " + e.getMessage());
            }
        }
    }
}
