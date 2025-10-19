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
import java.util.ArrayList;
import java.util.List;
import penaltyserver.config.DBConnection;

/**
 *
 * @author This PC
 */
public class PenaltyShotDAO {
    public static void recordShot(int matchId, int userId, int shotNumber, int direction, String result) {
        String sql = "INSERT INTO penalty_shots(match_id, user_id, shot_number, direction, result, shot_time) VALUES(?, ?, ?, ?, ?, NOW())";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, matchId);
            ps.setInt(2, userId);
            ps.setInt(3, shotNumber);
            ps.setInt(4, direction);
            ps.setString(5, result);
            ps.executeUpdate();
        }
        catch(SQLException e) {
            e.printStackTrace();
        }
    }
    public static List<PenaltyShot> getShotsByMatch(int matchId) {
        String sql = "SELECT * FROM penalty_shots WHRE match_id = ?";
        try(Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, matchId);
            ResultSet rs = ps.executeQuery();
            List<PenaltyShot> pList = new ArrayList<>();
        }
        catch(SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}