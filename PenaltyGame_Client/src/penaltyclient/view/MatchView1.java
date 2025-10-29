// penaltyclient.view.MatchView.java
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
import javafx.scene.control.ButtonType; // Import ButtonType
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import penaltyclient.controller.MatchController; // Import Controller

import java.util.Optional; // Import Optional

public class MatchView1 extends Application {

    private static final double SCENE_WIDTH = 900;
    private static final double SCENE_HEIGHT = 700;

    private Pane gamePane;
    private Rectangle[] goalZones = new Rectangle[6];
    private Circle ball;
    private Group goalkeeperGroup; // Nhóm thủ môn để dễ di chuyển và animation
    private Ellipse goalkeeperBody;
    private Line leftArm, rightArm;

    private Label scoreLabel;
    private Label messageLabel;
    private Label timerLabel; // Label hiển thị thời gian
    private Label opponentNameLabel; // Label hiển thị tên đối thủ
    private Button confirmButton; // Đổi tên từ shootButton

    private int selectedZone = -1;
    private boolean inputEnabled = false;

    private MatchController controller; // Tham chiếu tới controller

    // Phương thức để MatchController set chính nó
    public void setController(MatchController controller) {
        this.controller = controller;
    }

    @Override
    public void start(Stage primaryStage) throws Exception { // Thêm throws Exception
        primaryStage.setTitle("Penalty Shootout");

        VBox root = new VBox();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #87CEEB, #90EE90);"); // Sky to grass gradient

        createGameView();
        HBox bottomUI = createBottomUI();

        root.getChildren().addAll(gamePane, bottomUI);
        VBox.setVgrow(gamePane, Priority.ALWAYS); // Cho gamePane chiếm hết không gian thừa

        Scene scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
        setupControls(scene);

        primaryStage.setScene(scene);
        primaryStage.setResizable(false);

         // Xử lý khi người dùng đóng cửa sổ
         primaryStage.setOnCloseRequest(e -> {
             e.consume(); // Ngăn cửa sổ đóng ngay lập tức
             Alert confirmExit = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to exit the match?");
             confirmExit.showAndWait().ifPresent(response -> {
                 if (response == ButtonType.OK) {
                     if (controller != null) {
                         controller.onWindowClose(); // Thông báo cho controller
                     }
                     primaryStage.close(); // Đóng cửa sổ
                 }
             });
         });

        primaryStage.show();
        // Không gọi resetField ở đây, chờ server thông báo
    }

    private void createGameView() {
        gamePane = new Pane();
        // Không setPrefSize cố định nữa để nó tự co giãn

        // Sân cỏ (chiếm phần lớn)
        Rectangle field = new Rectangle(SCENE_WIDTH, SCENE_HEIGHT - 100); // Kích thước lớn hơn
        field.setFill(createGrassPattern());
        gamePane.getChildren().add(field);

        createFieldMarkings();
        createGoal();
        createGoalkeeper(); // Tạo thủ môn trước bóng
        createBall();       // Tạo bóng sau cùng để nó đè lên

        // Đảm bảo khung thành và lưới được vẽ sau sân cỏ nhưng trước thủ môn/bóng
        // (Thứ tự thêm vào gamePane quyết định lớp vẽ)
    }

     private Paint createGrassPattern() {
         // Gradient đơn giản hơn cho sân cỏ
         return new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                 new Stop(0, Color.rgb(50, 150, 50)),
                 new Stop(1, Color.rgb(40, 120, 40))
         );
     }

    private void createFieldMarkings() {
        // Vẽ các đường vạch trắng đơn giản
        // Vòng cấm
        Rectangle penaltyArea = new Rectangle(SCENE_WIDTH * 0.15, SCENE_HEIGHT * 0.1, SCENE_WIDTH * 0.7, SCENE_HEIGHT * 0.5);
        styleLine(penaltyArea);
        // Vùng 5m50
        Rectangle goalArea = new Rectangle(SCENE_WIDTH * 0.3, SCENE_HEIGHT * 0.1, SCENE_WIDTH * 0.4, SCENE_HEIGHT * 0.2);
        styleLine(goalArea);
        // Chấm penalty
        Circle penaltySpot = new Circle(SCENE_WIDTH / 2, SCENE_HEIGHT * 0.4, 5, Color.WHITE);
         // Nửa vòng tròn 16m50
         Arc penaltyArc = new Arc(SCENE_WIDTH / 2, SCENE_HEIGHT * 0.6, SCENE_WIDTH * 0.15, SCENE_HEIGHT*0.1, 0, -180);
         penaltyArc.setType(ArcType.OPEN);
         styleLine(penaltyArc);


        gamePane.getChildren().addAll(penaltyArea, goalArea, penaltySpot, penaltyArc);
    }

    // Helper để style đường kẻ
    private void styleLine(Shape shape) {
        shape.setStroke(Color.WHITE);
        shape.setStrokeWidth(3);
        if (!(shape instanceof Circle)) { // Circle không cần fill transparent
            shape.setFill(Color.TRANSPARENT);
        }
    }


    private void createGoal() {
        double goalWidth = SCENE_WIDTH * 0.3; // 30% chiều rộng sân
        double goalHeight = SCENE_HEIGHT * 0.2; // 20% chiều cao sân
        double goalX = (SCENE_WIDTH - goalWidth) / 2; // Căn giữa
        double goalY = SCENE_HEIGHT * 0.1; // Cách đỉnh 10%

        // Cột dọc và xà ngang
        Rectangle leftPost = new Rectangle(goalX - 5, goalY, 10, goalHeight);
        Rectangle rightPost = new Rectangle(goalX + goalWidth - 5, goalY, 10, goalHeight);
        Rectangle crossbar = new Rectangle(goalX - 5, goalY - 5, goalWidth + 10, 10);
        leftPost.setFill(Color.WHITE);
        rightPost.setFill(Color.WHITE);
        crossbar.setFill(Color.WHITE);

        // Lưới (vẽ đơn giản)
        Rectangle netBg = new Rectangle(goalX, goalY, goalWidth, goalHeight);
        netBg.setFill(Color.rgb(200, 200, 200, 0.4)); // Màu lưới mờ

        gamePane.getChildren().addAll(netBg, leftPost, rightPost, crossbar); // Add lưới và cột trước zone

        // Tạo 6 zone
        double zoneWidth = goalWidth / 3;
        double zoneHeight = goalHeight / 2;
        String[] zoneNames = {"Top Left", "Top Center", "Top Right",
                             "Bottom Left", "Bottom Center", "Bottom Right"};

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                final int index = row * 3 + col;
                goalZones[index] = new Rectangle(goalX + col * zoneWidth, goalY + row * zoneHeight, zoneWidth, zoneHeight);
                goalZones[index].setFill(Color.TRANSPARENT);
                goalZones[index].setStroke(Color.YELLOW);
                goalZones[index].setStrokeWidth(1);
                goalZones[index].setOpacity(0.4); // Hơi mờ để thấy vị trí
                goalZones[index].getStrokeDashArray().addAll(5d, 5d); // Nét đứt

                goalZones[index].setOnMouseEntered(e -> {
                    if (inputEnabled && selectedZone != index) {
                        goalZones[index].setFill(Color.rgb(255, 255, 0, 0.3)); // Highlight vàng khi hover
                        goalZones[index].setOpacity(0.7);
                    }
                });
                goalZones[index].setOnMouseExited(e -> {
                    if (inputEnabled && selectedZone != index) {
                        goalZones[index].setFill(Color.TRANSPARENT);
                        goalZones[index].setOpacity(0.4);
                    }
                });
                goalZones[index].setOnMouseClicked(e -> {
                    if (inputEnabled && controller != null) {
                        controller.onZoneSelected(index); // Báo cho controller biết zone được chọn
                    }
                });
                gamePane.getChildren().add(goalZones[index]);
            }
        }
    }

    private void createGoalkeeper() {
         goalkeeperBody = new Ellipse(SCENE_WIDTH / 2, SCENE_HEIGHT * 0.1 + SCENE_HEIGHT * 0.2 * 0.7, 15, 25); // Vị trí gần khung thành
         goalkeeperBody.setFill(Color.BLUE); // Màu áo thủ môn
         goalkeeperBody.setStroke(Color.DARKBLUE);
         goalkeeperBody.setStrokeWidth(2);

         leftArm = new Line();
         rightArm = new Line();
         updateGoalkeeperArms(); // Đặt vị trí ban đầu cho tay
         leftArm.setStroke(Color.BLUE);
         leftArm.setStrokeWidth(5);
         rightArm.setStroke(Color.BLUE);
         rightArm.setStrokeWidth(5);

         goalkeeperGroup = new Group(goalkeeperBody, leftArm, rightArm);
         gamePane.getChildren().add(goalkeeperGroup);
     }

     // Cập nhật vị trí tay thủ môn dựa trên vị trí cơ thể
     private void updateGoalkeeperArms() {
         double bodyX = goalkeeperBody.getCenterX();
         double bodyY = goalkeeperBody.getCenterY();
         leftArm.setStartX(bodyX);
         leftArm.setStartY(bodyY);
         leftArm.setEndX(bodyX - 20); // Tay dang ra
         leftArm.setEndY(bodyY - 10);
         rightArm.setStartX(bodyX);
         rightArm.setStartY(bodyY);
         rightArm.setEndX(bodyX + 20); // Tay dang ra
         rightArm.setEndY(bodyY - 10);
     }

    private void createBall() {
        ball = new Circle(SCENE_WIDTH / 2, SCENE_HEIGHT * 0.4, 12); // Đặt ở chấm penalty
        // Ball gradient
        RadialGradient ballGradient = new RadialGradient(0, 0, 0.3, 0.3, 0.6, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE), new Stop(1, Color.LIGHTGRAY));
        ball.setFill(ballGradient);
        ball.setStroke(Color.BLACK);
        ball.setStrokeWidth(1);
        gamePane.getChildren().add(ball);
    }

    private HBox createBottomUI() {
        HBox bottomPanel = new HBox(20); // Giảm khoảng cách
        bottomPanel.setAlignment(Pos.CENTER);
        bottomPanel.setPadding(new Insets(15));
        bottomPanel.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6);"); // Nền mờ hơn
        bottomPanel.setPrefHeight(100);

        opponentNameLabel = new Label("Opponent: ???"); // Hiển thị tên đối thủ
        opponentNameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        opponentNameLabel.setTextFill(Color.CYAN);

        scoreLabel = new Label("Score: 0 - 0");
        scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20)); // Cỡ chữ to hơn
        scoreLabel.setTextFill(Color.WHITE);

        timerLabel = new Label("Time: --"); // Hiển thị thời gian
        timerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        timerLabel.setTextFill(Color.YELLOW);
        timerLabel.setMinWidth(80); // Đặt chiều rộng tối thiểu
        timerLabel.setAlignment(Pos.CENTER);

        confirmButton = new Button("CONFIRM");
        confirmButton.setPrefSize(130, 45); // To hơn chút
        confirmButton.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        confirmButton.setStyle(
                "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
        confirmButton.setOnAction(e -> {
            if (inputEnabled && controller != null) {
                controller.onConfirmChoice();
            }
        });
        confirmButton.setDisable(true); // Ban đầu disable

        messageLabel = new Label("Waiting for match...");
        messageLabel.setFont(Font.font("Arial", 14));
        messageLabel.setTextFill(Color.LIGHTGREEN);
        messageLabel.setMinWidth(300); // Rộng hơn để chứa thông báo dài
        messageLabel.setAlignment(Pos.CENTER_LEFT);

        // Thêm spacer để đẩy messageLabel sang trái
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bottomPanel.getChildren().addAll(opponentNameLabel, scoreLabel, timerLabel, confirmButton, spacer, messageLabel);
        return bottomPanel;
    }

    // --- Các phương thức public để Controller gọi ---

    public void enableChoosingZone() {
        inputEnabled = true;
        selectedZone = -1; // Reset lựa chọn
        confirmButton.setDisable(true); // Chưa chọn nên chưa confirm được
         // Reset style các zone (xóa highlight cũ nếu có)
         for (int i = 0; i < 6; i++) {
             resetZoneStyle(i);
             goalZones[i].setMouseTransparent(false); // Cho phép click lại
         }
        // Có thể thêm hiệu ứng nhẹ nhàng cho các zone để báo hiệu có thể chọn
    }

    public void disableInput() {
        inputEnabled = false;
        confirmButton.setDisable(true);
         // Làm mờ các zone hoặc vô hiệu hóa hover/click
         for (Rectangle zone : goalZones) {
             zone.setFill(Color.TRANSPARENT);
             zone.setOpacity(0.2); // Mờ đi
             zone.setMouseTransparent(true); // Không nhận event chuột
         }
    }

    // Highlight ô được chọn
    public void highlightSelectedZone(int index) {
         if (index < 0 || index >= 6) return;
         selectedZone = index; // Lưu lại zone đang được chọn
         for (int i = 0; i < 6; i++) {
             if (i == index) {
                 goalZones[i].setFill(Color.rgb(0, 255, 0, 0.4)); // Màu xanh lá cây đậm hơn
                 goalZones[i].setStroke(Color.LIME);
                 goalZones[i].setOpacity(0.8);
                 goalZones[i].getStrokeDashArray().clear(); // Nét liền
             } else {
                resetZoneStyle(i); // Reset các ô khác
             }
         }
         confirmButton.setDisable(false); // Cho phép nhấn Confirm
     }

     // Reset style của một zone về mặc định
     private void resetZoneStyle(int index) {
         if (index < 0 || index >= 6) return;
         goalZones[index].setFill(Color.TRANSPARENT);
         goalZones[index].setStroke(Color.YELLOW);
         goalZones[index].setOpacity(0.4);
         goalZones[index].getStrokeDashArray().setAll(5d, 5d); // Nét đứt
     }


    public void updateMessage(String message) {
        Platform.runLater(() -> messageLabel.setText(message));
    }

    public void updateScore(int myScore, int opponentScore) {
        Platform.runLater(() -> scoreLabel.setText("Score: " + myScore + " - " + opponentScore));
    }

    public void updateTimer(int seconds) {
         Platform.runLater(() -> {
             if (seconds >= 0) {
                 timerLabel.setText("Time: " + String.format("%02d", seconds));
             } else {
                 timerLabel.setText("Time: --"); // Hết giờ hoặc không đếm
             }
         });
     }

     public void updateOpponentName(String name) {
         Platform.runLater(() -> opponentNameLabel.setText("Opponent: " + name));
     }

    // Animation sút bóng
    public void playShootAnimation(int shooterZone, int keeperZone, boolean isGoal, Runnable onFinish) {
        Platform.runLater(() -> {
             // Tính toán vị trí đích của bóng dựa vào shooterZone
             Point targetPoint = getZoneCenter(shooterZone);
             double targetX = targetPoint.x;
             double targetY = targetPoint.y;

             // Animation di chuyển và thu nhỏ bóng
             Timeline ballAnimation = new Timeline(
                     new KeyFrame(Duration.ZERO,
                             new KeyValue(ball.centerXProperty(), ball.getCenterX()), // Vị trí hiện tại
                             new KeyValue(ball.centerYProperty(), ball.getCenterY()),
                             new KeyValue(ball.radiusProperty(), 12)),
                     new KeyFrame(Duration.millis(500), // Thời gian bay
                             new KeyValue(ball.centerXProperty(), targetX),
                             new KeyValue(ball.centerYProperty(), targetY),
                             new KeyValue(ball.radiusProperty(), 8)) // Bóng nhỏ lại khi vào gôn
             );

             // Animation thủ môn (chỉ chạy nếu không phải là bàn thắng)
             if (!isGoal) {
                 Point keeperTarget = getZoneCenter(keeperZone); // Thủ môn bay đến ô đã chọn
                 animateGoalkeeper(keeperTarget.x, keeperTarget.y);
             }

             ballAnimation.setOnFinished(e -> {
                 // Có thể thêm hiệu ứng (lưới rung, bóng bật ra...)
                 if (onFinish != null) {
                     // Delay một chút trước khi gọi onFinish để người dùng kịp nhìn kết quả
                     PauseTransition pause = new PauseTransition(Duration.millis(500));
                     pause.setOnFinished(event -> onFinish.run());
                     pause.play();
                 }
             });
             ballAnimation.play();
        });
    }

    // Animation cho thủ môn (khi bắt bóng)
    public void playGoalkeeperAnimation(int shooterZone, int keeperZone, boolean isGoal, Runnable onFinish) {
         Platform.runLater(() -> {
             // Thủ môn sẽ bay đến ô keeperZone đã chọn
             Point keeperTarget = getZoneCenter(keeperZone);
             animateGoalkeeper(keeperTarget.x, keeperTarget.y);

             // Bóng cũng bay đến ô shooterZone
             Point ballTarget = getZoneCenter(shooterZone);
             Timeline ballAnimation = new Timeline(
                 new KeyFrame(Duration.ZERO,
                         new KeyValue(ball.centerXProperty(), ball.getCenterX()),
                         new KeyValue(ball.centerYProperty(), ball.getCenterY()),
                         new KeyValue(ball.radiusProperty(), 12)),
                 new KeyFrame(Duration.millis(500),
                         new KeyValue(ball.centerXProperty(), ballTarget.x),
                         new KeyValue(ball.centerYProperty(), ballTarget.y),
                         new KeyValue(ball.radiusProperty(), 8))
             );


             ballAnimation.setOnFinished(e -> {
                // Delay chút rồi gọi onFinish
                PauseTransition pause = new PauseTransition(Duration.millis(500));
                 pause.setOnFinished(event -> onFinish.run());
                 pause.play();
             });
             ballAnimation.play();

         });
     }

     // Hàm thực hiện animation di chuyển thủ môn
     private void animateGoalkeeper(double targetX, double targetY) {
         // Animation di chuyển cả nhóm thủ môn
         TranslateTransition tt = new TranslateTransition(Duration.millis(400), goalkeeperGroup);
         // Tính toán độ dịch chuyển cần thiết từ vị trí hiện tại
         double currentX = goalkeeperGroup.getTranslateX() + goalkeeperBody.getCenterX(); // Vị trí gốc + dịch chuyển hiện tại
         double currentY = goalkeeperGroup.getTranslateY() + goalkeeperBody.getCenterY();
         tt.setByX(targetX - currentX); // Dịch chuyển thêm để đạt target
         tt.setByY(targetY - currentY);
         tt.setInterpolator(Interpolator.EASE_OUT); // Di chuyển mượt

        // Có thể thêm animation tay dang rộng ra khi bay
         tt.play();
     }

    // Reset vị trí bóng, thủ môn, và các zone
    public void resetField() {
        Platform.runLater(() -> {
            // Reset bóng về chấm penalty
            ball.setCenterX(SCENE_WIDTH / 2);
            ball.setCenterY(SCENE_HEIGHT * 0.4);
            ball.setRadius(12);

             // Reset vị trí thủ môn (về giữa và xóa translate)
             goalkeeperGroup.setTranslateX(0);
             goalkeeperGroup.setTranslateY(0);
             goalkeeperBody.setCenterX(SCENE_WIDTH / 2); // Đặt lại gốc nếu cần
             goalkeeperBody.setCenterY(SCENE_HEIGHT * 0.1 + SCENE_HEIGHT * 0.2 * 0.7);
             updateGoalkeeperArms(); // Cập nhật lại tay về vị trí cũ

            // Reset zone styles và input
            selectedZone = -1;
            confirmButton.setDisable(true);
            inputEnabled = false; // Mặc định disable input sau mỗi lượt, chờ server cho phép
             for (int i = 0; i < 6; i++) {
                 resetZoneStyle(i);
                 goalZones[i].setMouseTransparent(true); // Disable click ban đầu
             }
        });
    }

    // Lấy tọa độ tâm của zone
    private Point getZoneCenter(int zoneIndex) {
         if (zoneIndex < 0 || zoneIndex >= 6) {
             // Trả về vị trí giữa gôn nếu index không hợp lệ
             return new Point((int)(SCENE_WIDTH / 2), (int)(SCENE_HEIGHT * 0.1 + SCENE_HEIGHT * 0.1));
         }
         double zoneWidth = goalZones[0].getWidth();
         double zoneHeight = goalZones[0].getHeight();
         double zoneX = goalZones[zoneIndex].getX();
         double zoneY = goalZones[zoneIndex].getY();
         return new Point((int)(zoneX + zoneWidth / 2), (int)(zoneY + zoneHeight / 2));
     }

     // Lớp tiện ích lưu tọa độ
     private static class Point {
         int x, y;
         Point(int x, int y) { this.x = x; this.y = y; }
     }


    public void showMatchEndMessage(String message) {
         Platform.runLater(() -> {
             Alert alert = new Alert(Alert.AlertType.INFORMATION);
             alert.setTitle("Match Over");
             alert.setHeaderText(null);
             alert.setContentText(message);
             // Thêm nút OK để đóng
             alert.showAndWait();
             // Sau khi người dùng nhấn OK, có thể đóng cửa sổ hoặc quay về lobby
             // if(controller != null) controller.onWindowClose(); // Ví dụ
             // Hoặc chờ server gửi lệnh mới
         });
     }

     public void showErrorMessage(String message) {
         Platform.runLater(() -> {
             Alert alert = new Alert(Alert.AlertType.ERROR);
             alert.setTitle("Error");
             alert.setHeaderText(null);
             alert.setContentText(message);
             alert.showAndWait();
              // Có thể đóng cửa sổ sau khi báo lỗi nghiêm trọng
              // if(controller != null) controller.onWindowClose();
         });
     }

      // Xử lý sự kiện nhấn phím
      private void setupControls(Scene scene) {
         scene.setOnKeyPressed(e -> {
             if (!inputEnabled) return; // Nếu input bị khóa thì không xử lý

             switch (e.getCode()) {
                 case SPACE:
                 case ENTER: // Thêm Enter để confirm
                     if (controller != null) {
                         controller.onConfirmChoice();
                     }
                     break;
                 // Các phím số hoặc Numpad để chọn zone nhanh
                 case DIGIT1: case NUMPAD1: controller.onZoneSelected(3); break; // Bottom Left
                 case DIGIT2: case NUMPAD2: controller.onZoneSelected(4); break; // Bottom Center
                 case DIGIT3: case NUMPAD3: controller.onZoneSelected(5); break; // Bottom Right
                 case DIGIT4: case NUMPAD4: controller.onZoneSelected(0); break; // Top Left
                 case DIGIT5: case NUMPAD5: controller.onZoneSelected(1); break; // Top Center
                 case DIGIT6: case NUMPAD6: controller.onZoneSelected(2); break; // Top Right
                 default:
                     // Không làm gì với các phím khác
                     break;
             }
         });
     }

    // --- Main method (chỉ để test giao diện độc lập, không cần cho game chính) ---
    public static void main(String[] args) {
        launch(args);
    }

    // Bỏ phần initComponents() vì dùng JavaFX code thuần
}