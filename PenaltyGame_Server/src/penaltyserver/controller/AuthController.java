/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package penaltyserver.controller;
import java.sql.*;
import penaltyserver.config.DBConnection;
import penaltyserver.model.User;
/**
 *
 * @author This PC
 */
public class AuthController {

    public AuthController() {
    }

    public static boolean checkLogin(User user) {
        try(Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                int userId = rs.getInt("user_id");
                user.setUserId(userId);
                return true;
            }
            return false;
            
        }
        catch(SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    
}