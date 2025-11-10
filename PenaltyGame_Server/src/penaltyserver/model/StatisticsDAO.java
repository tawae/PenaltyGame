package penaltyserver.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Import các lớp DTO từ project "common" của bạn
import share.MatchHistoryRecord;
import share.RankingData;

import penaltyserver.config.DBConnection; // Import kết nối CSDL


public class StatisticsDAO {

    public List<MatchHistoryRecord> getMatchHistory(int userId) {
        List<MatchHistoryRecord> historyList = new ArrayList<>();
        
        // Câu SQL này JOIN 4 bảng (tự JOIN match_results 2 lần)
        // để tìm thông tin của chính mình, thông tin của đối thủ, và thông tin trận đấu
        String sql = "SELECT " +
                     "  m.match_id, " +
                     "  m.start_time, " +
                     "  mr_self.score AS my_score, " +
                     "  mr_opp.score AS opponent_score, " +
                     "  mr_self.is_winner, " +
                     "  u_opp.username AS opponent_username " +
                     "FROM " +
                     "  match_results AS mr_self " +
                     "JOIN " +
                     "  match_results AS mr_opp ON mr_self.match_id = mr_opp.match_id " +
                     "                         AND mr_self.user_id != mr_opp.user_id " +
                     "JOIN " +
                     "  users AS u_opp ON mr_opp.user_id = u_opp.user_id " +
                     "JOIN " +
                     "  matches AS m ON mr_self.match_id = m.match_id " +
                     "WHERE " +
                     "  mr_self.user_id = ? " +
                     "ORDER BY " +
                     "  m.start_time DESC;"; // Sắp xếp trận mới nhất lên đầu

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                MatchHistoryRecord record = new MatchHistoryRecord(
                    rs.getInt("match_id"),
                    rs.getString("opponent_username"),
                    rs.getInt("my_score"),
                    rs.getInt("opponent_score"),
                    rs.getBoolean("is_winner"),
                    rs.getTimestamp("start_time") // Timestamp tự động chuyển thành java.util.Date
                );
                historyList.add(record);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return historyList;
    }


    public List<RankingData> getLeaderboard() {
        List<RankingData> leaderboard = new ArrayList<>();
        
        // Câu SQL này nhóm tất cả người chơi, tính tổng điểm và tổng số trận thắng
        // Sắp xếp theo mô tả của bạn
        String sql = "SELECT " +
                     "  u.username, " +
                     "  SUM(mr.score) AS total_score, " +
                     "  SUM(mr.is_winner) AS total_wins " + // SUM(boolean) sẽ đếm số lần 'true' (giá trị 1)
                     "FROM " +
                     "  match_results AS mr " +
                     "JOIN " +
                     "  users AS u ON mr.user_id = u.user_id " +
                     "GROUP BY " +
                     "  mr.user_id, u.username " +
                     "ORDER BY " +
                     "  total_score DESC, " + // Tiêu chí 1: Tổng điểm giảm dần
                     "  total_wins DESC;";   // Tiêu chí 2: Tổng thắng giảm dần

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ResultSet rs = ps.executeQuery();

            int rank = 1; // Bắt đầu đếm hạng từ 1
            while (rs.next()) {
                RankingData ranking = new RankingData(
                    rank++,
                    rs.getString("username"),
                    rs.getInt("total_score"),
                    rs.getInt("total_wins")
                );
                leaderboard.add(ranking);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return leaderboard;
    }
}