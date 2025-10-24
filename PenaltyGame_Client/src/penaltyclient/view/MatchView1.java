package penaltyclient.view;

import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import penaltyclient.controller.MatchController;
import network.ClientNetwork;

public class MatchView1 extends Application {

    private static final double SCENE_WIDTH = 900;
    private static final double SCENE_HEIGHT = 700;
    
    private Pane gamePane;
    private Rectangle[] goalZones = new Rectangle[6];
    private Circle ball;
    private Ellipse goalkeeper;
    private Label scoreLabel;
    private Label messageLabel;
    private Button shootButton;
    
    private int selectedZone = -1;
    private boolean inputEnabled = false;
    
    private MatchController controller;
    private ClientNetwork network;
    private String playerName;
    
    private void connectToServer(Stage primaryStage) {
        network = new ClientNetwork();
        
        if (network.connect(playerName)) {
            initializeGameUI(primaryStage);
            
            // Create controller
            controller = new MatchController(this, network, playerName);
            
            // Join matchmaking queue
            network.joinQueue();
            updateMessage("Waiting for opponent...");
            
        } else {
            showErrorDialog("Could not connect to server!");
            Platform.exit();
        }
    }
    
    private void initializeGameUI(Stage primaryStage) {
        primaryStage.setTitle("Penalty Shootout - " + playerName);
        
        VBox root = new VBox();
        root.setStyle("-fx-background-color: #87CEEB;");
        
        createGameView();        
        HBox bottomUI = createBottomUI();
        
        root.getChildren().addAll(gamePane, bottomUI);
        
        Scene scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
        setupControls(scene);
        
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
        
        primaryStage.setOnCloseRequest(e -> {
            if (network != null) {
                network.disconnect();
            }
        });
    }
    
    public void setController(MatchController controller) {
        this.controller = controller;
    }
    
    private void createGameView() {
        gamePane = new Pane();
        gamePane.setPrefSize(SCENE_WIDTH, SCENE_HEIGHT - 100);
        
        // Sky background
        Rectangle sky = new Rectangle(0, 0, SCENE_WIDTH, 200);
        Stop[] skyStops = new Stop[] {
            new Stop(0, Color.rgb(135, 206, 235)),
            new Stop(1, Color.rgb(173, 216, 230))
        };
        sky.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, skyStops));
        
        // Stadium
        Rectangle stadium = new Rectangle(0, 100, SCENE_WIDTH, 100);
        stadium.setFill(Color.rgb(100, 100, 150, 0.3));
        
        // Grass field
        Polygon field = new Polygon();
        field.getPoints().addAll(new Double[]{
            0.0, 200.0, SCENE_WIDTH, 200.0,
            SCENE_WIDTH, SCENE_HEIGHT-100.0, 0.0, SCENE_HEIGHT-100.0
        });
        field.setFill(createGrassPattern());
        
        gamePane.getChildren().addAll(sky, stadium, field);
        
        createFieldMarkings();
        createGoal();
        createGoalkeeper();
        createBall();
    }
    
    private Paint createGrassPattern() {
        Stop[] stops = new Stop[] {
            new Stop(0, Color.rgb(46, 125, 50)),
            new Stop(0.15, Color.rgb(56, 142, 60)),
            new Stop(0.3, Color.rgb(46, 125, 50)),
            new Stop(0.45, Color.rgb(56, 142, 60)),
            new Stop(0.6, Color.rgb(46, 125, 50)),
            new Stop(0.75, Color.rgb(56, 142, 60)),
            new Stop(0.9, Color.rgb(46, 125, 50)),
            new Stop(1, Color.rgb(56, 142, 60))
        };
        return new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, stops);
    }
    
    private void createFieldMarkings() {
        Polygon penaltyArea = new Polygon();
        penaltyArea.getPoints().addAll(new Double[]{
            200.0, 250.0, 700.0, 250.0, 800.0, 400.0, 100.0, 400.0
        });
        penaltyArea.setFill(Color.TRANSPARENT);
        penaltyArea.setStroke(Color.WHITE);
        penaltyArea.setStrokeWidth(3);
        
        Circle penaltySpot = new Circle(450, 420, 5);
        penaltySpot.setFill(Color.WHITE);
        
        gamePane.getChildren().addAll(penaltyArea, penaltySpot);
    }
    
    private void createGoal() {
        double goalX = 300;
        double goalY = 150;
        double goalWidth = 300;
        double goalHeight = 100;
        
        Rectangle leftPost = new Rectangle(goalX - 5, goalY, 10, goalHeight);
        leftPost.setFill(Color.WHITE);
        
        Rectangle rightPost = new Rectangle(goalX + goalWidth - 5, goalY, 10, goalHeight);
        rightPost.setFill(Color.WHITE);
        
        Rectangle crossbar = new Rectangle(goalX, goalY - 5, goalWidth, 10);
        crossbar.setFill(Color.WHITE);
        
        Rectangle netBg = new Rectangle(goalX, goalY, goalWidth, goalHeight);
        netBg.setFill(Color.rgb(200, 200, 200, 0.3));
        
        // Create 6 zones
        double zoneWidth = goalWidth / 3;
        double zoneHeight = goalHeight / 2;
        
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                final int index = row * 3 + col;
                
                goalZones[index] = new Rectangle(
                    goalX + col * zoneWidth,
                    goalY + row * zoneHeight,
                    zoneWidth, zoneHeight
                );
                
                goalZones[index].setFill(Color.TRANSPARENT);
                goalZones[index].setStroke(Color.YELLOW);
                goalZones[index].setStrokeWidth(2);
                goalZones[index].setOpacity(0);
                
                goalZones[index].setOnMouseEntered(e -> {
                    if (inputEnabled && selectedZone == -1) {
                        goalZones[index].setOpacity(0.3);
                        goalZones[index].setFill(Color.rgb(255, 255, 0, 0.2));
                    }
                });
                
                goalZones[index].setOnMouseExited(e -> {
                    if (inputEnabled && selectedZone != index) {
                        goalZones[index].setOpacity(0);
                        goalZones[index].setFill(Color.TRANSPARENT);
                    }
                });
                
                goalZones[index].setOnMouseClicked(e -> {
                    if (inputEnabled) {
                        selectZone(index);
                    }
                });
                
                gamePane.getChildren().add(goalZones[index]);
            }
        }
        
        gamePane.getChildren().addAll(netBg, leftPost, rightPost, crossbar);
    }
    
    private void createGoalkeeper() {
        goalkeeper = new Ellipse(450, 200, 15, 20);
        goalkeeper.setFill(Color.ORANGE);
        goalkeeper.setStroke(Color.DARKORANGE);
        goalkeeper.setStrokeWidth(2);
        gamePane.getChildren().add(goalkeeper);
    }
    
    private void createBall() {
        ball = new Circle(450, 420, 12);
        Stop[] ballStops = new Stop[] {
            new Stop(0, Color.WHITE),
            new Stop(0.9, Color.rgb(240, 240, 240)),
            new Stop(1, Color.rgb(200, 200, 200))
        };
        ball.setFill(new RadialGradient(0, 0, 0.3, 0.3, 0.5, true, 
                                       CycleMethod.NO_CYCLE, ballStops));
        ball.setStroke(Color.BLACK);
        ball.setStrokeWidth(1);
        gamePane.getChildren().add(ball);
    }
    
    private HBox createBottomUI() {
        HBox bottomPanel = new HBox(30);
        bottomPanel.setAlignment(Pos.CENTER);
        bottomPanel.setPadding(new Insets(20));
        bottomPanel.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");
        bottomPanel.setPrefHeight(100);
        
        scoreLabel = new Label("Score: 0 - 0");
        scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        scoreLabel.setTextFill(Color.WHITE);
        
        shootButton = new Button("CONFIRM");
        shootButton.setPrefSize(120, 40);
        shootButton.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        shootButton.setStyle(
            "-fx-background-color: #4CAF50; " +
            "-fx-text-fill: white; " +
            "-fx-background-radius: 5;"
        );
        shootButton.setDisable(true);
        shootButton.setOnAction(e -> {
            if (controller != null) {
                controller.onConfirmChoice();
            }
        });
        
        messageLabel = new Label("Waiting for match...");
        messageLabel.setFont(Font.font("Arial", 14));
        messageLabel.setTextFill(Color.YELLOW);
        messageLabel.setMinWidth(300);
               
        bottomPanel.getChildren().addAll(scoreLabel, shootButton, messageLabel);
        return bottomPanel;
    }
    
    private void selectZone(int index) {
        selectedZone = index;
        
        for (int i = 0; i < 6; i++) {
            if (i == index) {
                goalZones[i].setOpacity(0.5);
                goalZones[i].setFill(Color.rgb(0, 255, 0, 0.3));
                goalZones[i].setStroke(Color.LIME);
            } else {
                goalZones[i].setOpacity(0);
                goalZones[i].setFill(Color.TRANSPARENT);
            }
        }
        
        if (controller != null) {
            controller.onZoneSelected(index);
        }
        
        shootButton.setDisable(false);
    }
    
    public void enableShooterMode() {
        inputEnabled = true;
        selectedZone = -1;
        shootButton.setDisable(true);
        for (Rectangle zone : goalZones) {
            zone.setOpacity(0);
        }
    }
    
    public void enableGoalkeeperMode() {
        inputEnabled = true;
        selectedZone = -1;
        shootButton.setDisable(true);
        for (Rectangle zone : goalZones) {
            zone.setOpacity(0);
        }
    }
    
    public void disableInput() {
        inputEnabled = false;
        shootButton.setDisable(true);
    }
    
    public void updateMessage(String message) {
        Platform.runLater(() -> messageLabel.setText(message));
    }
    
    public void updateScore(int myScore, int opponentScore) {
        Platform.runLater(() -> {
            scoreLabel.setText("Score: " + myScore + " - " + opponentScore);
        });
    }
    
    public void playShootAnimation(int shooterZone, int keeperZone, boolean isGoal, Runnable onFinish) {
        Platform.runLater(() -> {
            double goalX = 300;
            double goalY = 150;
            double zoneWidth = 100;
            double zoneHeight = 50;
            
            int row = shooterZone / 3;
            int col = shooterZone % 3;
            
            double targetX = goalX + col * zoneWidth + zoneWidth / 2;
            double targetY = goalY + row * zoneHeight + zoneHeight / 2;
            
            Timeline animation = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(ball.centerXProperty(), 450),
                    new KeyValue(ball.centerYProperty(), 420),
                    new KeyValue(ball.radiusProperty(), 12)
                ),
                new KeyFrame(Duration.millis(600),
                    new KeyValue(ball.centerXProperty(), targetX),
                    new KeyValue(ball.centerYProperty(), targetY),
                    new KeyValue(ball.radiusProperty(), 6)
                )
            );
            
            // Keeper animation
            int keeperRow = keeperZone / 3;
            int keeperCol = keeperZone % 3;
            double keeperX = goalX + keeperCol * zoneWidth + zoneWidth / 2;
            
            Timeline keeperAnim = new Timeline(
                new KeyFrame(Duration.millis(400),
                    new KeyValue(goalkeeper.centerXProperty(), keeperX)
                )
            );
            
            animation.setOnFinished(e -> {
                if (onFinish != null) onFinish.run();
            });
            
            animation.play();
            keeperAnim.play();
        });
    }
    
    public void playGoalkeeperAnimation(int shooterZone, int keeperZone, boolean isGoal, Runnable onFinish) {
        playShootAnimation(shooterZone, keeperZone, isGoal, onFinish);
    }
    
    public void resetField() {
        Platform.runLater(() -> {
            ball.setCenterX(450);
            ball.setCenterY(420);
            ball.setRadius(12);
            goalkeeper.setCenterX(450);
            selectedZone = -1;
            
            for (Rectangle zone : goalZones) {
                zone.setOpacity(0);
                zone.setFill(Color.TRANSPARENT);
                zone.setStroke(Color.YELLOW);
            }
        });
    }
    
    public void showMatchStartMessage(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Match Start");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    public void showMatchEndMessage(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Match End");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    public void showErrorMessage(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    private void setupControls(Scene scene) {
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case SPACE:
                    if (controller != null && inputEnabled) {
                        controller.onConfirmChoice();
                    }
                    break;
                case ESCAPE:
                    if (network != null) {
                        network.disconnect();
                    }
                    Platform.exit();
                    break;
                default:
                    if (inputEnabled && e.getCode().isDigitKey()) {
                        try {
                            int zone = Integer.parseInt(e.getText()) - 1;
                            if (zone >= 0 && zone < 6) {
                                selectZone(zone);
                            }
                        } catch (NumberFormatException ex) {
                            // Ignore
                        }
                    }
                    break;
            }
        });
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}