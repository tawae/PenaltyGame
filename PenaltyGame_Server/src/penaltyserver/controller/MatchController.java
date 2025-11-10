package penaltyserver.controller;

import java.io.ObjectOutputStream;
import java.sql.Timestamp;
import java.util.*;
import penaltyserver.PenaltyServer;
import penaltyserver.model.ClientHandler;
import penaltyserver.model.Match;
import penaltyserver.model.MatchDAO;
import penaltyserver.model.MatchResult;
import penaltyserver.model.MatchResultDAO;
import penaltyserver.model.PenaltyShotDAO;
import penaltyserver.model.SessionManager;
import penaltyserver.model.UserDAO;
import penaltyserver.model.User;

public class MatchController {
    private Map<String, Match> activeMatches;
    
    public MatchController() {
        this.activeMatches = new HashMap<>();
    }
    
    public void createAndStartMatch(User player1, User player2) {
        
        String matchId = String.valueOf(matchDAO.createMatch(player1.getUserId()));
        
        if (matchId.equals("-1")) {
            System.err.println("Error creating match in database.");
            // Gửi lỗi về cho người chơi
            ClientHandler handler1 = SessionManager.getSession(player1.getUsername());
            ClientHandler handler2 = SessionManager.getSession(player2.getUsername());
            if(handler1 != null) handler1.sendMessage("MATCH_FAIL:Server database error");
            if(handler2 != null) handler2.sendMessage("MATCH_FAIL:Server database error");
            return;
        }

        mrDAO.addPlayerToMatch(Integer.parseInt(matchId), player1.getUserId());
        mrDAO.addPlayerToMatch(Integer.parseInt(matchId), player2.getUserId());
        // Lấy ClientHandler của 2 người chơi từ SessionManager
        ClientHandler handler1 = SessionManager.getSession(player1.getUsername());
        ClientHandler handler2 = SessionManager.getSession(player2.getUsername());

        if (handler1 == null || handler2 == null) {
            System.err.println("Error starting match: One or both players are not online.");
            // Có thể gửi thông báo lỗi về client nếu cần
            if(handler1 != null) handler1.sendMessage("MATCH_FAIL:Opponent not found");
            if(handler2 != null) handler2.sendMessage("MATCH_FAIL:Opponent not found");
            return;
        }

        // Tạo đối tượng Match mới
        Match match = new Match(matchId, player1, player2, handler1, handler2);
        activeMatches.put(matchId, match);

        player1.setCurrentMatchId(matchId);
        player2.setCurrentMatchId(matchId);

        System.out.println("Match created [" + matchId + "]: " + player1.getUsername() + " vs " + player2.getUsername());

        startMatchLogic(match);
    }
    
    private void startMatchLogic(Match match) {
        // Random người sút trước
        User firstShooter;
        if (Math.random() < 0.5) {
            match.setCurrentShooter(match.getPlayer1());
            match.setCurrentKeeper(match.getPlayer2());
        } else {
            match.setCurrentShooter(match.getPlayer2());
            match.setCurrentKeeper(match.getPlayer1());
        }
        firstShooter = match.getCurrentShooter();

        // Gửi thông báo MATCH_START cho cả hai
        match.sendToPlayer(match.getPlayer1(), "MATCH_START:" + match.getPlayer2().getUsername() + ":" + firstShooter.getUsername());
        match.sendToPlayer(match.getPlayer2(), "MATCH_START:" + match.getPlayer1().getUsername() + ":" + firstShooter.getUsername());

        System.out.println("Match [" + match.getMatchId() + "] starting: " + firstShooter.getUsername() + " shoots first.");

        // Bắt đầu lượt đầu tiên sau delay
        Timer startTimer = new Timer();
        startTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                startTurn(match); // Gọi hàm bắt đầu lượt của controller
            }
        }, 2000);
    }
    
    private void startTurn(Match match) {
        // Hủy timer cũ (nếu có) của trận đấu này
        Timer oldTimer = match.getTurnTimer();
        if (oldTimer != null) {
            oldTimer.cancel();
        }

        // Reset lựa chọn
        match.setShooterChoice(null);
        match.setKeeperChoice(null);

        User currentShooter = match.getCurrentShooter();
        User currentKeeper = match.getCurrentKeeper();

        // Gửi thông báo TURN_START
        String roleShooter = "SHOOTER";
        String roleKeeper = "GOALKEEPER";
        match.sendToPlayer(currentShooter, "TURN_START:" + match.getCurrentRound() + ":" + roleShooter);
        match.sendToPlayer(currentKeeper, "TURN_START:" + match.getCurrentRound() + ":" + roleKeeper);

        System.out.println("Match [" + match.getMatchId() + "] Round " + match.getCurrentRound() + ": "
                + currentShooter.getUsername() + " (" + roleShooter + "), "
                + currentKeeper.getUsername() + " (" + roleKeeper + ")");

        // Bắt đầu timer mới
        Timer turnTimer = new Timer();
        match.setTurnTimer(turnTimer); // Lưu timer vào đối tượng Match
        turnTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Match [" + match.getMatchId() + "] Round " + match.getCurrentRound() + " - Time's up!");
                handleTimeout(match); // Gọi hàm xử lý timeout của controller
            }
        }, 15000); // 15 giây
    }
    
    private synchronized void handleTimeout(Match match) {
        // Kiểm tra xem lượt này đã được xử lý chưa (tránh gọi processTurn nhiều lần)
        if (match.getTurnTimer() == null) {
            System.out.println("Match [" + match.getMatchId() + "] - Timeout ignored, turn already processed.");
            return; // Lượt đã được xử lý (ví dụ: cả 2 đã chọn)
        }
        match.setTurnTimer(null); // Đánh dấu timer đã xử lý (hoặc bị hủy)

        boolean choiceMadeByTimeout = false;
        if (match.getShooterChoice() == null) {
            match.setShooterChoice((int) (Math.random() * 6));
            System.out.println("Match [" + match.getMatchId() + "]: " + match.getCurrentShooter().getUsername() + " timeout, random choice: " + match.getShooterChoice());
            choiceMadeByTimeout = true;
        }
        if (match.getKeeperChoice() == null) {
            match.setKeeperChoice((int) (Math.random() * 6));
            System.out.println("Match [" + match.getMatchId() + "]: " + match.getCurrentKeeper().getUsername() + " timeout, random choice: " + match.getKeeperChoice());
            choiceMadeByTimeout = true;
        }

        // Chỉ gọi processTurn nếu timeout thực sự đã tạo ra lựa chọn cuối cùng
        if (choiceMadeByTimeout) {
            processTurn(match);
        }
    }
    
    public synchronized void handlePlayerChoice(User user, int zoneChoice) {
        String matchId = user.getCurrentMatchId();
        if (matchId == null) {
            System.err.println("Error handling choice: User " + user.getUsername() + " is not in a match.");
            return;
        }
        Match match = activeMatches.get(matchId);
        if (match == null) {
            System.err.println("Error handling choice: Match not found for ID " + matchId);
            user.setCurrentMatchId(null);
            return;
        }

        if (match.getTurnTimer() == null) {
            System.out.println("Match [" + matchId + "]: Choice from " + user.getUsername() + " ignored, turn already processed or timed out.");
            return;
        }

        User currentShooter = match.getCurrentShooter();
        User currentKeeper = match.getCurrentKeeper();

        boolean choiceRegistered = false;
        if (user.equals(currentShooter)) {
            if (match.getShooterChoice() == null) {
                match.setShooterChoice(zoneChoice);
                System.out.println("Match [" + matchId + "]: " + currentShooter.getUsername() + " chose zone " + zoneChoice);
                match.sendToPlayer(currentShooter, "WAITING_FOR_OPPONENT");
                choiceRegistered = true;
            }
        } else if (user.equals(currentKeeper)) {
            if (match.getKeeperChoice() == null) {
                match.setKeeperChoice(zoneChoice);
                System.out.println("Match [" + matchId + "]: " + currentKeeper.getUsername() + " chose zone " + zoneChoice);
                match.sendToPlayer(currentKeeper, "WAITING_FOR_OPPONENT");
                choiceRegistered = true;
            }
        } else {
            System.err.println("Error in Match [" + matchId + "]: User " + user.getUsername() + " is not part of this turn.");
            return;
        }

        if (!choiceRegistered) {
            System.out.println("Match [" + matchId + "]: " + user.getUsername() + " attempted to choose again.");
        }

        // Nếu cả hai đã chọn, xử lý lượt ngay và hủy timer
        if (match.getShooterChoice() != null && match.getKeeperChoice() != null) {
            Timer timer = match.getTurnTimer();
            if (timer != null) {
                timer.cancel();
                match.setTurnTimer(null); // Đánh dấu timer đã bị hủy
                System.out.println("Match [" + matchId + "]: Both players chose, processing turn.");
                processTurn(match); // Gọi hàm xử lý của controller
            } else {
                // Trường hợp hiếm: Cả hai chọn gần như cùng lúc timeout xảy ra
                System.out.println("Match [" + matchId + "]: Both players chose, but turn might have already timed out or processed.");
            }
        }
    }
    
    private void processTurn(Match match) {
        Integer shooterChoice = match.getShooterChoice();
        Integer keeperChoice = match.getKeeperChoice();
        User currentShooter = match.getCurrentShooter();

        // Kiểm tra null phòng trường hợp gọi hàm này khi chưa đủ lựa chọn (dù không nên xảy ra)
        if (shooterChoice == null || keeperChoice == null) {
            System.err.println("Error processing turn for Match [" + match.getMatchId() + "]: Choices not complete.");
            return;
        }

        boolean isGoal = (!shooterChoice.equals(keeperChoice));
        String resultStr = isGoal ? "goal" : "save";

        if (isGoal) {
            match.incrementScore(currentShooter); // Cập nhật điểm trong model Match
        }
        
        try {
            int dbMatchId = Integer.parseInt(match.getMatchId());
            int shooterUserId = currentShooter.getUserId();
            int shotNumber = match.getCurrentRound(); // Lấy round hiện tại làm số thứ tự cú sút
            int direction = shooterChoice; // Lấy lựa chọn của người sút

            psDAO.recordShot(dbMatchId, shooterUserId, shotNumber, direction, resultStr);

        } catch (NumberFormatException e) {
            System.err.println("Error saving shot to DB: Invalid Match ID format " + match.getMatchId());
        }

        System.out.println("Match [" + match.getMatchId() + "] Turn Result: Shooter(" + shooterChoice + ") vs Keeper(" + keeperChoice + ") -> " + resultStr);
        System.out.println("Match [" + match.getMatchId() + "] Score: " + match.getPlayer1().getUsername() + " " + match.getPlayer1Score() + " - " + match.getPlayer2Score() + " " + match.getPlayer2().getUsername());

        // Gửi kết quả
        String resultP1 = "TURN_RESULT:" + shooterChoice + ":" + keeperChoice + ":" + resultStr + ":"
                + match.getPlayer1Score() + ":" + match.getPlayer2Score() + ":" + currentShooter.getUsername();
        String resultP2 = "TURN_RESULT:" + shooterChoice + ":" + keeperChoice + ":" + resultStr + ":"
                + match.getPlayer2Score() + ":" + match.getPlayer1Score() + ":" + currentShooter.getUsername();

        match.sendToPlayer(match.getPlayer1(), resultP1);
        match.sendToPlayer(match.getPlayer2(), resultP2);

        // Lên lịch kiểm tra và chuyển lượt sau delay
        Timer nextTurnTimer = new Timer();
        nextTurnTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                checkAndProceed(match);
            }
        }, 3000);
    }
    
    private void checkAndProceed(Match match) {
        boolean roundCompleted = match.getCurrentKeeper().equals(match.getPlayer1()); // Lượt bắt của P1 vừa xong

        if (match.isSuddenDeath()) {
            if (roundCompleted && match.getPlayer1Score() != match.getPlayer2Score()) {
                endMatch(match, match.getPlayer1Score() > match.getPlayer2Score() ? match.getPlayer1() : match.getPlayer2());
            } else {
                if (roundCompleted) {
                    match.setCurrentRound(match.getCurrentRound() + 1); // Vẫn tăng round trong sudden death
                }
                swapRolesAndNextTurn(match);
            }
        } else { // Normal rounds
            if (match.getCurrentRound() == match.getMaxNormalRounds() && roundCompleted) {
                if (match.getPlayer1Score() == match.getPlayer2Score()) {
                    match.setSuddenDeath(true);
                    System.out.println("Match [" + match.getMatchId() + "] entering Sudden Death.");
                    match.setCurrentRound(match.getCurrentRound() + 1); 
                    swapRolesAndNextTurn(match);
                } else {
                    endMatch(match, match.getPlayer1Score() > match.getPlayer2Score() ? match.getPlayer1() : match.getPlayer2());
                }
            } else {
                if (roundCompleted) {
                    match.setCurrentRound(match.getCurrentRound() + 1);
                }
                swapRolesAndNextTurn(match);
            }
        }
    }
    
    private void swapRolesAndNextTurn(Match match) {
        match.swapRoles(); // Đổi vai trò trong model Match
        startTurn(match); // Bắt đầu lượt mới
    }

    // Kết thúc trận đấu cho Match cụ thể
    private void endMatch(Match match, User winner) {
        String winnerUsername = (winner != null) ? winner.getUsername() : "DRAW";
        match.setMatchStatus("finished");
        match.setEndTime(new Timestamp(System.currentTimeMillis())); // Ghi thời gian kết thúc
        System.out.println("Match [" + match.getMatchId() + "] ended. Winner: " + winnerUsername);

        // Hủy timer nếu còn
        Timer timer = match.getTurnTimer();
        if (timer != null) {
            timer.cancel();
            match.setTurnTimer(null);
        }
        
        // luu vao csdl
        try {
            int dbMatchId = Integer.parseInt(match.getMatchId());

            // 4.1. Cập nhật bảng 'matches' (set end_time, status)
            matchDAO.finishMatch(dbMatchId);

            // 4.2. Cập nhật điểm số cuối cùng cho cả 2 người chơi
            mrDAO.updateScore(dbMatchId, match.getPlayer1().getUserId(), match.getPlayer1Score());
            mrDAO.updateScore(dbMatchId, match.getPlayer2().getUserId(), match.getPlayer2Score());

            // 4.3. Đánh dấu người chiến thắng (nếu có)
            if (winner != null) {
                mrDAO.setWinner(dbMatchId, winner.getUserId());
            }

        } catch (NumberFormatException e) {
            System.err.println("Error saving match end to DB: Invalid Match ID format " + match.getMatchId());
        }
        // Gửi thông báo kết thúc
        match.sendToPlayer(match.getPlayer1(), "MATCH_END:" + winnerUsername + ":" + match.getPlayer1Score() + ":" + match.getPlayer2Score());
        match.sendToPlayer(match.getPlayer2(), "MATCH_END:" + winnerUsername + ":" + match.getPlayer2Score() + ":" + match.getPlayer1Score());

        // Dọn dẹp
        activeMatches.remove(match.getMatchId());
        if (match.getPlayer1() != null) {
            match.getPlayer1().setCurrentMatchId(null);
        }
        if (match.getPlayer2() != null) {
            match.getPlayer2().setCurrentMatchId(null);
        }
        // Lưu DB nếu cần
    }
    
    public void handlePlayerDisconnect(User user) {
        String matchId = user.getCurrentMatchId();
        if (matchId == null) {
            return; // Không trong trận nào
        }
        Match match = activeMatches.get(matchId);
        if (match == null) {
            return; // Trận không tồn tại hoặc đã kết thúc
        }
        System.out.println("Handling disconnect for " + user.getUsername() + " in match " + matchId);

        // Hủy timer nếu còn
        Timer timer = match.getTurnTimer();
        if (timer != null) {
            timer.cancel();
            match.setTurnTimer(null);
        }

        // Lấy người chơi còn lại (người chiến thắng)
        User otherPlayer = match.getOtherPlayer(user);

        if (otherPlayer != null) {
            // Gửi thông báo cho người còn lại rằng đối thủ đã thoát
            ClientHandler otherHandler = SessionManager.getSession(otherPlayer.getUsername());
            if (otherHandler != null) { // Kiểm tra xem người kia còn online không
                otherHandler.sendMessage("OPPONENT_DISCONNECTED");
            }

            // Set tỉ số cứng là 5-0 cho người ở lại
            if (otherPlayer.equals(match.getPlayer1())) {
                match.setPlayer1Score(5);
                match.setPlayer2Score(0);
            } else {
                match.setPlayer2Score(5);
                match.setPlayer1Score(0);
            }

            // Gọi endMatch để lưu CSDL (với tỉ số 5-0) và gửi kết quả
            // endMatch sẽ tự động dọn dẹp activeMatches và reset matchId cho cả 2
            endMatch(match, otherPlayer); 

        } else {
            // Trường hợp hiếm: không tìm thấy người chơi kia (ví dụ: cả 2 thoát gần như cùng lúc)
            // Chỉ dọn dẹp mà không lưu
            activeMatches.remove(matchId);
            user.setCurrentMatchId(null); // Reset matchId người disconnect
            System.out.println("Match [" + matchId + "] removed. No other player found.");
        }

        System.out.println("Match [" + matchId + "] processed disconnect for " + user.getUsername());
    }

    // Inner class representing a match between two players
    private static MatchDAO matchDAO = new MatchDAO();
    private static MatchResultDAO mrDAO = new MatchResultDAO();
    private static UserDAO userDAO = new UserDAO();
    private static PenaltyShotDAO psDAO = new PenaltyShotDAO();
}
