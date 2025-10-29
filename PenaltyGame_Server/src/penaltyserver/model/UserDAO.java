/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package penaltyserver.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import penaltyserver.config.DBConnection;

/**
 *
 * @author This PC
 */
public class UserDAO {
    public int getUserIdByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                int userId = rs.getInt("user_id");
                return userId;
            }
        }
        catch(SQLException e) {
            e.printStackTrace();
        } 
        return -1;
    }
}
