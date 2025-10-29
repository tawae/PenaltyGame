package penaltyclient.view;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Callback;
import penaltyclient.controller.LobbyController;
import penaltyclient.model.Player; 
import penaltyclient.model.MatchRecord; // Import mới
import penaltyclient.model.RankingEntry; // Import mới

/**
 * Giao diện Lobby được xây dựng bằng JavaFX, với 3 Tab.
 */
public class LobbyView {

    private LobbyController lobbyController;
    private BorderPane rootPane;
    private String username;

    // Tab 1: Online Players
    private TableView<Player> tblPlayers;
    private ObservableList<Player> playerList; 
    private Button btnReloadPlayers;

    // Tab 2: Match History
    private TableView<MatchRecord> tblMatchHistory;
    private ObservableList<MatchRecord> historyList;

    // Tab 3: Ranking
    private TableView<RankingEntry> tblRanking;
    private ObservableList<RankingEntry> rankingList;

    public LobbyView(String username, LobbyController lobbyController) {
        this.username = username;
        this.lobbyController = lobbyController;
        createLobbyPane();
    }

    private void createLobbyPane() {
        rootPane = new BorderPane();
        rootPane.setStyle("-fx-background-color: #f0f0f0;");

        // === 1. HEADER (GIỮ NGUYÊN) ===
        HBox headerPanel = new HBox(10);
        headerPanel.setAlignment(Pos.CENTER_LEFT);
        headerPanel.setPadding(new Insets(10));
        headerPanel.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");

        Text lblTitle = new Text("Penalty Game Lobby");
        lblTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-fill: #2E7D32;"); 

        HBox userPanel = new HBox(10);
        userPanel.setAlignment(Pos.CENTER_RIGHT);
        Label lblUserInfo = new Label("Welcome: " + username);
        lblUserInfo.setStyle("-fx-font-size: 14px; -fx-font-style: italic;");
        
        Button btnLogout = new Button("Logout");
        btnLogout.setStyle(
            "-fx-background-color: #D32F2F; -fx-text-fill: white; " +
            "-fx-font-weight: bold; -fx-background-radius: 5;"
        );
        btnLogout.setOnAction(e -> this.lobbyController.handleLogout());
        userPanel.getChildren().addAll(lblUserInfo, btnLogout);

        HBox.setHgrow(userPanel, Priority.ALWAYS); 
        headerPanel.getChildren().addAll(lblTitle, userPanel);
        
        rootPane.setTop(headerPanel);

        // === 2. TAB PANE (PHẦN THAY ĐỔI CHÍNH) ===
        TabPane mainTabPane = new TabPane();
        mainTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE); // Không cho phép đóng tab

        // --- Tab 1: Online Players ---
        Tab tabLobby = new Tab("Lobby");
        tabLobby.setContent(createOnlinePlayersPane());
        
        // --- Tab 2: Match History ---
        Tab tabHistory = new Tab("Lịch sử đấu");
        tabHistory.setContent(createMatchHistoryPane());
        // Thêm sự kiện để load data khi nhấn vào tab
        tabHistory.setOnSelectionChanged(e -> {
            if (tabHistory.isSelected()) {
                lobbyController.loadMatchHistory();
            }
        });

        // --- Tab 3: Ranking ---
        Tab tabRanking = new Tab("Bảng xếp hạng");
        tabRanking.setContent(createRankingPane());
        // Thêm sự kiện để load data khi nhấn vào tab
        tabRanking.setOnSelectionChanged(e -> {
            if (tabRanking.isSelected()) {
                lobbyController.loadRanking();
            }
        });

        mainTabPane.getTabs().addAll(tabLobby, tabHistory, tabRanking);
        
        rootPane.setCenter(mainTabPane);
    }

    // --- Pane cho Tab 1: Online Players ---
    private Parent createOnlinePlayersPane() {
        BorderPane lobbyPane = new BorderPane();
        lobbyPane.setPadding(new Insets(10));

        // Nút Reload
        btnReloadPlayers = new Button("Reload 🔄"); // 🔄
        btnReloadPlayers.setStyle("-fx-font-weight: bold; -fx-background-color: #007bff; -fx-text-fill: white; -fx-background-radius: 5;");
        btnReloadPlayers.setOnAction(e -> lobbyController.handleReloadPlayers());
        
        HBox topBox = new HBox(btnReloadPlayers);
        topBox.setAlignment(Pos.CENTER_RIGHT);
        topBox.setPadding(new Insets(0, 0, 10, 0)); // Cách bảng 10px
        lobbyPane.setTop(topBox);

        // Bảng Player
        setupPlayersTable();
        ScrollPane scrollPane = new ScrollPane(tblPlayers);
        scrollPane.setFitToWidth(true);
        lobbyPane.setCenter(scrollPane);
        
        return lobbyPane;
    }

    // --- Pane cho Tab 2: Match History ---
    private Parent createMatchHistoryPane() {
        BorderPane historyPane = new BorderPane();
        historyPane.setPadding(new Insets(10));
        
        setupMatchHistoryTable();
        ScrollPane scrollPane = new ScrollPane(tblMatchHistory);
        scrollPane.setFitToWidth(true);
        historyPane.setCenter(scrollPane);

        return historyPane;
    }

    // --- Pane cho Tab 3: Ranking ---
    private Parent createRankingPane() {
        BorderPane rankingPane = new BorderPane();
        rankingPane.setPadding(new Insets(10));
        
        setupRankingTable();
        ScrollPane scrollPane = new ScrollPane(tblRanking);
        scrollPane.setFitToWidth(true);
        rankingPane.setCenter(scrollPane);

        return rankingPane;
    }


    // === CÁC HÀM KHỞI TẠO BẢNG ===

    // Khởi tạo bảng Online Players (Đã có)
    private void setupPlayersTable() {
        playerList = FXCollections.observableArrayList();
        tblPlayers = new TableView<>(playerList);
        tblPlayers.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); 

        TableColumn<Player, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(new PropertyValueFactory<Player, String>("name")); 

        TableColumn<Player, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<Player, String>("status"));

        TableColumn<Player, Integer> colScore = new TableColumn<>("Score");
        colScore.setCellValueFactory(new PropertyValueFactory<Player, Integer>("score"));

        TableColumn<Player, Void> colAction = new TableColumn<>("Action");
        colAction.setCellFactory(createButtonCellFactory()); 

        tblPlayers.getColumns().addAll(colName, colStatus, colScore, colAction);
    }
    
    // Khởi tạo bảng Match History (Mới)
    private void setupMatchHistoryTable() {
        historyList = FXCollections.observableArrayList();
        tblMatchHistory = new TableView<>(historyList);
        tblMatchHistory.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<MatchRecord, String> colOpponent = new TableColumn<>("Đối thủ");
        colOpponent.setCellValueFactory(new PropertyValueFactory<MatchRecord, String>("opponentName"));

        TableColumn<MatchRecord, String> colResult = new TableColumn<>("Kết quả");
        colResult.setCellValueFactory(new PropertyValueFactory<MatchRecord, String>("result"));

        TableColumn<MatchRecord, String> colDate = new TableColumn<>("Thời gian");
        colDate.setCellValueFactory(new PropertyValueFactory<MatchRecord, String>("date"));
        
        tblMatchHistory.getColumns().addAll(colOpponent, colResult, colDate);
        
        // Thêm dữ liệu giả để test
        // historyList.add(new MatchRecord("PlayerB", "Win (5-3)", "2025-10-28 10:30"));
        // historyList.add(new MatchRecord("PlayerC", "Loss (1-4)", "2025-10-27 09:15"));
    }

    // Khởi tạo bảng Ranking (Mới)
    private void setupRankingTable() {
        rankingList = FXCollections.observableArrayList();
        tblRanking = new TableView<>(rankingList);
        tblRanking.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<RankingEntry, Integer> colRank = new TableColumn<>("Hạng");
        colRank.setCellValueFactory(new PropertyValueFactory<RankingEntry, Integer>("rank"));

        TableColumn<RankingEntry, String> colUser = new TableColumn<>("Người chơi");
        colUser.setCellValueFactory(new PropertyValueFactory<RankingEntry, String>("username"));

        TableColumn<RankingEntry, Integer> colScore = new TableColumn<>("Điểm");
        colScore.setCellValueFactory(new PropertyValueFactory<RankingEntry, Integer>("score"));
        
        TableColumn<RankingEntry, Integer> colWins = new TableColumn<>("Số trận thắng");
        colWins.setCellValueFactory(new PropertyValueFactory<RankingEntry, Integer>("wins"));

        tblRanking.getColumns().addAll(colRank, colUser, colScore, colWins);
        
        // Thêm dữ liệu giả để test
        // rankingList.add(new RankingEntry(1, "BestPlayer", 1500, 50));
        // rankingList.add(new RankingEntry(2, "PlayerA", 1400, 45));
    }


    // === CÁC HÀM CẬP NHẬT DỮ LIỆU ===
    
    // Thêm người chơi vào Tab 1
    public void addPlayer(String name, String status, int score) {
        Player player = new Player(name, status, score);
        Platform.runLater(new Runnable() { 
            @Override
            public void run() {
                playerList.add(player);
            }
        });
    }

    // Xóa tất cả người chơi khỏi Tab 1 (cho việc reload)
    public void clearOnlinePlayers() {
        Platform.runLater(new Runnable() { 
            @Override
            public void run() {
                playerList.clear();
            }
        });
    }

    // Cập nhật bảng Lịch sử đấu (Tab 2)
    public void updateMatchHistory(java.util.List<MatchRecord> records) {
         Platform.runLater(new Runnable() { 
            @Override
            public void run() {
                historyList.clear();
                historyList.addAll(records);
            }
        });
    }

    // Cập nhật bảng Xếp hạng (Tab 3)
    public void updateRanking(java.util.List<RankingEntry> entries) {
         Platform.runLater(new Runnable() { 
            @Override
            public void run() {
                rankingList.clear();
                rankingList.addAll(entries);
            }
        });
    }

    // Trả về node root của view này
    public Parent getView() {
        return rootPane;
    }
    
    // Hàm tạo nút "Invite" (Không đổi)
    private Callback<TableColumn<Player, Void>, TableCell<Player, Void>> createButtonCellFactory() {
        return new Callback<TableColumn<Player, Void>, TableCell<Player, Void>>() {
            @Override
            public TableCell<Player, Void> call(final TableColumn<Player, Void> param) {
                final TableCell<Player, Void> cell = new TableCell<Player, Void>() {
                    private final Button btn = new Button("Invite");
                    {
                        btn.setStyle(
                            "-fx-background-color: #4CAF50; -fx-text-fill: white; " +
                            "-fx-font-weight: bold; -fx-background-radius: 5;"
                        );
                        btn.setOnAction(event -> {
                            Player player = getTableView().getItems().get(getIndex());
                            lobbyController.handleInvite(player.getName());
                            btn.setText("Invited"); 
                            btn.setDisable(true);   
                        });
                    }
                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
                return cell;
            }
        };
    }
}