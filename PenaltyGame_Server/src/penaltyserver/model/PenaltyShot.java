/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package penaltyserver.model;

import java.sql.Timestamp;

/**
 *
 * @author This PC
 */
public class PenaltyShot {
    private int shotId;
    private int matchId;
    private int userId;
    private int shotNumber;
    private int direction; // 1-6;
    private String result; // goal, miss, save
    private Timestamp shotTime;

    public PenaltyShot(int matchId, int userId, int shotNumber, int direction) {
        this.matchId = matchId;
        this.userId = userId;
        this.shotNumber = shotNumber;
        this.direction = direction;
    }

    public int getShotId() {
        return shotId;
    }

    public void setShotId(int shotId) {
        this.shotId = shotId;
    }

    public int getMatchId() {
        return matchId;
    }

    public void setMatchId(int matchId) {
        this.matchId = matchId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getShotNumber() {
        return shotNumber;
    }

    public void setShotNumber(int shotNumber) {
        this.shotNumber = shotNumber;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Timestamp getShotTime() {
        return shotTime;
    }

    public void setShotTime(Timestamp shotTime) {
        this.shotTime = shotTime;
    }
}
