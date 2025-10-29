/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package penaltyserver.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import penaltyserver.config.DBConnection;

/**
 *
 * @author This PC
 */
public class MatchResultDAO {
    public static void addPlayerToMatch(int matchId, int userId) {
        String sql = "INSERT INTO match_results(match_id, user_id) VALUES(?, ?)";
        try(Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, matchId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
        catch(SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static void updateScore(int matchId, int userId, int score) {
        String sql = "UPDATE match_results SET score = ? WHERE match_id = ? AND user_id = ?";
        try(Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, score);
            ps.setInt(2, matchId);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
        catch(SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static void setWinner(int matchId, int userId) {
        String sql = "UPDATE match_results SET is_winner = TRUE WHERE match_id = ? AND user_id = ?";
        try(Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, matchId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } 
        catch(SQLException e) {
            e.printStackTrace();
        }
    }
}
