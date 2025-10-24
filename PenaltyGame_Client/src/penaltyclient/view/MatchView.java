/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package penaltyclient.view;

import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import javafx.util.Duration;

import penaltyclient.controller.MatchController;


public class MatchView extends Application {

    private static final double SCENE_WIDTH = 900;
    private static final double SCENE_HEIGHT = 700;
    
    private Pane gamePane;
    private Rectangle[] goalZones = new Rectangle[6];
    private Circle ball;
    private Ellipse goalkeeper;
    private Label scoreLabel;
    private Label messageLabel;
    private Button shootButton;
    
    private boolean inputEnabled = false;
    private int selectedZone = -1;
    private int goals = 0;
    private int attempts = 0;
    
    private MatchController controller;
    private String playerName;
    private String opponentName;
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Penalty Shootout - Football Game");
        
        // Main container
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
    }
    
    private void createGameView() {
        gamePane = new Pane();
        gamePane.setPrefSize(SCENE_WIDTH, SCENE_HEIGHT - 100);
        
        // Sky background (1/3 of screen)
        Rectangle sky = new Rectangle(0, 0, SCENE_WIDTH, 200);
        Stop[] skyStops = new Stop[] {
            new Stop(0, Color.rgb(135, 206, 235)),
            new Stop(1, Color.rgb(173, 216, 230))
        };
        sky.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, skyStops));
        
        // Stadium/crowd area (simple representation)
        Rectangle stadium = new Rectangle(0, 100, SCENE_WIDTH, 100);
        stadium.setFill(Color.rgb(100, 100, 150, 0.3));
        
        // Grass field (2/3 of screen with perspective)
        Polygon field = new Polygon();
        field.getPoints().addAll(new Double[]{
            0.0, 200.0,                    // Top left
            SCENE_WIDTH, 200.0,            // Top right  
            SCENE_WIDTH, SCENE_HEIGHT-100.0,  // Bottom right
            0.0, SCENE_HEIGHT-100.0        // Bottom left
        });
        field.setFill(createGrassPattern());
        
        gamePane.getChildren().addAll(sky, stadium, field);
        
        createFieldMarkings();
        createGoal();
        createGoalkeeper();
        createBall();
    }
    
    private Paint createGrassPattern() {
        // Grass with stripes
        Stop[] stops = new Stop[] {
            new Stop(0, Color.rgb(46, 125, 50)),
            new Stop(0.15, Color.rgb(46, 125, 50)),
            new Stop(0.15, Color.rgb(56, 142, 60)),
            new Stop(0.3, Color.rgb(56, 142, 60)),
            new Stop(0.3, Color.rgb(46, 125, 50)),
            new Stop(0.45, Color.rgb(46, 125, 50)),
            new Stop(0.45, Color.rgb(56, 142, 60)),
            new Stop(0.6, Color.rgb(56, 142, 60)),
            new Stop(0.6, Color.rgb(46, 125, 50)),
            new Stop(0.75, Color.rgb(46, 125, 50)),
            new Stop(0.75, Color.rgb(56, 142, 60)),
            new Stop(0.9, Color.rgb(56, 142, 60)),
            new Stop(0.9, Color.rgb(46, 125, 50)),
            new Stop(1, Color.rgb(46, 125, 50))
        };
        return new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, stops);
    }
    
    private void createFieldMarkings() {
        // Penalty area lines với perspective
        Polygon penaltyArea = new Polygon();
        penaltyArea.getPoints().addAll(new Double[]{
            200.0, 250.0,   // Top left
            700.0, 250.0,   // Top right
            800.0, 400.0,   // Bottom right
            100.0, 400.0    // Bottom left
        });
        penaltyArea.setFill(Color.TRANSPARENT);
        penaltyArea.setStroke(Color.WHITE);
        penaltyArea.setStrokeWidth(3);
        
        // Goal area (smaller box)
        Polygon goalArea = new Polygon();
        goalArea.getPoints().addAll(new Double[]{
            320.0, 250.0,
            580.0, 250.0,
            620.0, 320.0,
            280.0, 320.0
        });
        goalArea.setFill(Color.TRANSPARENT);
        goalArea.setStroke(Color.WHITE);
        goalArea.setStrokeWidth(3);
        
        // Penalty spot (chấm phạt đền) - đặt trong vòng cấm
        Circle penaltySpot = new Circle(450, 350, 5);  // đưa lên trong penaltyArea
        penaltySpot.setFill(Color.WHITE);

        // Penalty arc (nửa vòng tròn) - nằm ngoài vòng cấm
        Arc penaltyArc = new Arc(450, 400, 100, 60, 0, -180); 
        // tâm đặt ở chính giữa vạch 16m50 (y = 250), vẽ ra ngoài
        penaltyArc.setType(ArcType.OPEN);
        penaltyArc.setFill(Color.TRANSPARENT);
        penaltyArc.setStroke(Color.WHITE);
        penaltyArc.setStrokeWidth(3);

        gamePane.getChildren().addAll(penaltyArea, goalArea, penaltySpot, penaltyArc);
    }
    
    private void createGoal() {
        double goalX = 300;
        double goalY = 150;
        double goalWidth = 300;
        double goalHeight = 100;
        
        // Goal frame
        // Left post
        Rectangle leftPost = new Rectangle(goalX - 5, goalY, 10, goalHeight);
        leftPost.setFill(Color.WHITE);
        leftPost.setStroke(Color.GRAY);
        leftPost.setStrokeWidth(1);
        
        // Right post
        Rectangle rightPost = new Rectangle(goalX + goalWidth - 5, goalY, 10, goalHeight);
        rightPost.setFill(Color.WHITE);
        rightPost.setStroke(Color.GRAY);
        rightPost.setStrokeWidth(1);
        
        // Crossbar
        Rectangle crossbar = new Rectangle(goalX, goalY - 5, goalWidth, 10);
        crossbar.setFill(Color.WHITE);
        crossbar.setStroke(Color.GRAY);
        crossbar.setStrokeWidth(1);
        
        // Net background
        Rectangle netBg = new Rectangle(goalX, goalY, goalWidth, goalHeight);
        netBg.setFill(Color.rgb(200, 200, 200, 0.3));
        
        // Net pattern - vertical lines
        for (int i = 0; i <= 15; i++) {
            Line line = new Line(
                goalX + i * 20, goalY,
                goalX + i * 20, goalY + goalHeight
            );
            line.setStroke(Color.LIGHTGRAY);
            line.setStrokeWidth(1);
            line.setOpacity(0.5);
            gamePane.getChildren().add(line);
        }
        
        // Net pattern - horizontal lines
        for (int i = 0; i <= 5; i++) {
            Line line = new Line(
                goalX, goalY + i * 20,
                goalX + goalWidth, goalY + i * 20
            );
            line.setStroke(Color.LIGHTGRAY);
            line.setStrokeWidth(1);
            line.setOpacity(0.5);
            gamePane.getChildren().add(line);
        }
        
        // Create 6 shooting zones (3 columns x 2 rows)
        double zoneWidth = goalWidth / 3;
        double zoneHeight = goalHeight / 2;
        
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                final int index = row * 3 + col;
                
                goalZones[index] = new Rectangle(
                    goalX + col * zoneWidth,
                    goalY + row * zoneHeight,
                    zoneWidth,
                    zoneHeight
                );
                
                goalZones[index].setFill(Color.TRANSPARENT);
                goalZones[index].setStroke(Color.YELLOW);
                goalZones[index].setStrokeWidth(2);
                goalZones[index].setOpacity(0);
                
                // Zone labels
                String[] zoneNames = {"Top Left", "Top Center", "Top Right", 
                                     "Bottom Left", "Bottom Center", "Bottom Right"};
                
                // Mouse events
                final int zoneIndex = index;
                goalZones[index].setOnMouseEntered(e -> {
                    if (selectedZone == -1) {
                        goalZones[zoneIndex].setOpacity(0.3);
                        goalZones[zoneIndex].setFill(Color.rgb(255, 255, 0, 0.2));
                        messageLabel.setText("Aim: " + zoneNames[zoneIndex]);
                    }
                });
                
                goalZones[index].setOnMouseExited(e -> {
                    if (selectedZone == -1 || selectedZone != zoneIndex) {
                        goalZones[zoneIndex].setOpacity(0);
                        goalZones[zoneIndex].setFill(Color.TRANSPARENT);
                    }
                    if (selectedZone == -1) {
                        messageLabel.setText("Click on a goal zone to shoot!");
                    }
                });
                
                goalZones[index].setOnMouseClicked(e -> {
                    if (selectedZone == -1) {
                        selectZone(zoneIndex);
                    }
                });
                
                gamePane.getChildren().add(goalZones[index]);
            }
        }
        
        gamePane.getChildren().addAll(netBg, leftPost, rightPost, crossbar);
    }
    
    private void createGoalkeeper() {
        // Goalkeeper in center of goal
        goalkeeper = new Ellipse(450, 200, 15, 20);
        goalkeeper.setFill(Color.ORANGE);
        goalkeeper.setStroke(Color.DARKORANGE);
        goalkeeper.setStrokeWidth(2);
        
        // Arms
        Line leftArm = new Line(435, 200, 420, 190);
        leftArm.setStroke(Color.ORANGE);
        leftArm.setStrokeWidth(3);
        
        Line rightArm = new Line(465, 200, 480, 190);
        rightArm.setStroke(Color.ORANGE);
        rightArm.setStrokeWidth(3);
        
        gamePane.getChildren().addAll(goalkeeper, leftArm, rightArm);
    }
    
    private void createBall() {
        // Ball at penalty spot
        ball = new Circle(450, 350, 12);
        
        // Soccer ball pattern
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
        
        // Score display
        scoreLabel = new Label("Score: 0/0");
        scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        scoreLabel.setTextFill(Color.WHITE);
        
        // Shoot button
        shootButton = new Button("SHOOT!");
        shootButton.setPrefSize(120, 40);
        shootButton.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        shootButton.setStyle(
            "-fx-background-color: #4CAF50; " +
            "-fx-text-fill: white; " +
            "-fx-background-radius: 5; " +
            "-fx-cursor: hand;"
        );
//        shootButton.setOnAction(e -> showErrorMessage("clicked"));
        shootButton.setOnAction(e -> performShoot());
        
        // Message label
        messageLabel = new Label("Click on a goal zone to shoot!");
        messageLabel.setFont(Font.font("Arial", 14));
        messageLabel.setTextFill(Color.YELLOW);
        messageLabel.setMinWidth(250);
               
        bottomPanel.getChildren().addAll(scoreLabel, shootButton, messageLabel);
        
        return bottomPanel;
    }
    
    private void selectZone(int index) {
        selectedZone = index;
        
        // Highlight selected zone
        for (int i = 0; i < 6; i++) {
            if (i == index) {
                goalZones[i].setOpacity(0.5);
                goalZones[i].setFill(Color.rgb(0, 255, 0, 0.3));
                goalZones[i].setStroke(Color.LIME);
            } else {
                goalZones[i].setOpacity(0);
                goalZones[i].setFill(Color.TRANSPARENT);
                goalZones[i].setStroke(Color.YELLOW);
            }
        }
        
        messageLabel.setText("Target selected! Press SPACE or click SHOOT!");
        messageLabel.setTextFill(Color.LIME);
    }
    
    private void performShoot() {
        if (selectedZone == -1) {
            messageLabel.setText("Please select a target zone first!");
            messageLabel.setTextFill(Color.RED);
            return;
        }
        
        attempts++;
        
        // Calculate target position
        double goalX = 300;
        double goalY = 150;
        double zoneWidth = 300.0 / 3;
        double zoneHeight = 100.0 / 2;
        
        int row = selectedZone / 3;
        int col = selectedZone % 3;
        
        double targetX = goalX + col * zoneWidth + zoneWidth / 2;
        double targetY = goalY + row * zoneHeight + zoneHeight / 2;
        
        // Add some randomness
        targetX += (Math.random() - 0.5) * 20;
        targetY += (Math.random() - 0.5) * 10;
        
        // Ball shooting animation
        Timeline shootAnimation = new Timeline();
        
        KeyFrame start = new KeyFrame(Duration.ZERO,
            new KeyValue(ball.centerXProperty(), 450),
            new KeyValue(ball.centerYProperty(), 350),
            new KeyValue(ball.radiusProperty(), 12)
        );
        
        KeyFrame mid = new KeyFrame(Duration.millis(300),
            new KeyValue(ball.centerXProperty(), targetX),
            new KeyValue(ball.centerYProperty(), targetY - 30),
            new KeyValue(ball.radiusProperty(), 8)
        );
        
        KeyFrame end = new KeyFrame(Duration.millis(600),
            new KeyValue(ball.centerXProperty(), targetX),
            new KeyValue(ball.centerYProperty(), targetY),
            new KeyValue(ball.radiusProperty(), 6)
        );
        
        shootAnimation.getKeyFrames().addAll(start, mid, end);
        
        // Goalkeeper animation
        boolean saved = Math.random() < 0.3; // 30% save chance
        
        if (saved) {
            double keeperX = targetX;
            Timeline keeperAnimation = new Timeline(
                new KeyFrame(Duration.millis(400),
                    new KeyValue(goalkeeper.centerXProperty(), keeperX)
                )
            );
            keeperAnimation.play();
        }
        
        shootAnimation.setOnFinished(e -> {
            if (!saved) {
                goals++;
                messageLabel.setText("GOAL! Great shot!");
                messageLabel.setTextFill(Color.LIME);
            } else {
                messageLabel.setText("SAVED! The goalkeeper stopped it!");
                messageLabel.setTextFill(Color.RED);
            }
            
            scoreLabel.setText("Score: " + goals + "/" + attempts);
            
            // Reset after delay
            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(ev -> resetField());
            pause.play();
        });
        
        shootAnimation.play();
    }
    
    public void resetField() {
        // Reset ball
        ball.setCenterX(450);
        ball.setCenterY(350);
        ball.setRadius(12);
        
        // Reset goalkeeper
        goalkeeper.setCenterX(450);
        
        // Clear selection
        selectedZone = -1;
        for (Rectangle zone : goalZones) {
            zone.setOpacity(0);
            zone.setFill(Color.TRANSPARENT);
            zone.setStroke(Color.YELLOW);
        }
        
        messageLabel.setText("Click on a goal zone to shoot!");
        messageLabel.setTextFill(Color.YELLOW);
    }
    
    private void setupControls(Scene scene) {
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case SPACE:
                    performShoot();
                    break;
                case R:
                    resetField();
                    goals = 0;
                    attempts = 0;
                    scoreLabel.setText("Score: 0/0");
                    break;
                case ESCAPE:
                    Platform.exit();
                    break;
                default:
                    // Number keys 1-6 for quick zone selection
                    if (e.getCode().isDigitKey()) {
                        String text = e.getText();
                        try {
                            int zone = Integer.parseInt(text) - 1;
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
    
    public void enableChoosingZone() {
        inputEnabled = true;
        selectedZone = -1;
        shootButton.setDisable(true);
        for (Rectangle zone : goalZones) {
            zone.setOpacity(0);
        }
    }
    
    public int getSelectedZone(){
        return selectedZone;
    }
    
    public void updateMessage(String message) {
        Platform.runLater(() -> messageLabel.setText(message));
    }
    
    public void updateScore(int myScore, int opponentScore) {
        Platform.runLater(() -> {
            scoreLabel.setText("Score: " + myScore + " - " + opponentScore);
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
    
    public static void main(String[] args) {
        launch(args);
    }
}
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 300, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

