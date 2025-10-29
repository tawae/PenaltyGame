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
import java.sql.Statement;
import penaltyserver.config.DBConnection;

/**
 *
 * @author This PC
 */
public class MatchDAO {
    public static int createMatch(int createdBy) {
        String sql = "INSERT INTO matches(start_time, status, created_by) VALUES (NOW(), 'playing',?)";
        try(Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, createdBy);
            ps.executeUpdate();
            
            
            ResultSet rs = ps.getGeneratedKeys();
            if(rs.next()) {
                return rs.getInt(1);
            }
        }
        catch(SQLException e) {
            e.printStackTrace();
        }
                
        return -1;
    }
    
    public static void finishMatch(int matchId) {
        String sql = "UPDATE matches SET end_time = NOW(), status = 'finished' WHERE match_id = ?";
        try(Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, matchId);
            ps.executeUpdate();
        }
        catch(SQLException e) {
            e.printStackTrace();
        }
                
    }
    
    public static Match getMatchById(int matchId) {
        return null;
    }
}

