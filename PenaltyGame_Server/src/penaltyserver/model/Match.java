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
public class Match {
    private int matchId;
    private String matchStatus;
    private int createdBy;
    private Timestamp startTime;
    private Timestamp endTime;

    public Match(int createdBy) {
        this.createdBy = createdBy;
    }

    public String getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(String matchStatus) {
        this.matchStatus = matchStatus;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }
}