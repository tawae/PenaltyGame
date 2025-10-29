/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package penaltyclient.model;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
/**
 *
 * @author This PC
 */
public class Player {
    private final SimpleStringProperty name;
    private final SimpleStringProperty status;
    private final SimpleIntegerProperty score;

    public Player(String name, String status, int score) {
        this.name = new SimpleStringProperty(name);
        this.status = new SimpleStringProperty(status);
        this.score = new SimpleIntegerProperty(score);
    }

    // Getter cho Name
    public String getName() {
        return name.get();
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    // Getter cho Status
    public String getStatus() {
        return status.get();
    }

    public SimpleStringProperty statusProperty() {
        return status;
    }

    // Getter cho Score
    public int getScore() {
        return score.get();
    }

    public SimpleIntegerProperty scoreProperty() {
        return score;
    }
}
