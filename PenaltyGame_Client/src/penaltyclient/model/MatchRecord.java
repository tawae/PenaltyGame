package penaltyclient.model;

import javafx.beans.property.SimpleStringProperty;

/**
 * Lớp model đại diện cho một bản ghi trong Lịch sử đấu.
 */
public class MatchRecord {

    private final SimpleStringProperty opponentName;
    private final SimpleStringProperty result;
    private final SimpleStringProperty date;

    public MatchRecord(String opponentName, String result, String date) {
        this.opponentName = new SimpleStringProperty(opponentName);
        this.result = new SimpleStringProperty(result);
        this.date = new SimpleStringProperty(date);
    }

    // Getters
    public String getOpponentName() { return opponentName.get(); }
    public String getResult() { return result.get(); }
    public String getDate() { return date.get(); }

    // Properties (cần cho TableView)
    public SimpleStringProperty opponentNameProperty() { return opponentName; }
    public SimpleStringProperty resultProperty() { return result; }
    public SimpleStringProperty dateProperty() { return date; }
}