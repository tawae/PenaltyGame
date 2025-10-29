/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package penaltyclient.model;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Lớp model đại diện cho một người chơi trong Bảng xếp hạng.
 */
public class RankingEntry {

    private final SimpleIntegerProperty rank;
    private final SimpleStringProperty username;
    private final SimpleIntegerProperty score;
    private final SimpleIntegerProperty wins;

    public RankingEntry(int rank, String username, int score, int wins) {
        this.rank = new SimpleIntegerProperty(rank);
        this.username = new SimpleStringProperty(username);
        this.score = new SimpleIntegerProperty(score);
        this.wins = new SimpleIntegerProperty(wins);
    }

    // Getters
    public int getRank() { return rank.get(); }
    public String getUsername() { return username.get(); }
    public int getScore() { return score.get(); }
    public int getWins() { return wins.get(); }
    
    // Properties (cần cho TableView)
    public SimpleIntegerProperty rankProperty() { return rank; }
    public SimpleStringProperty usernameProperty() { return username; }
    public SimpleIntegerProperty scoreProperty() { return score; }
    public SimpleIntegerProperty winsProperty() { return wins; }
}