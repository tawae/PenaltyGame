# Penalty Shootout Game - Network Programming Project

## Introduction

This project is a multiplayer Penalty Shootout game developed using **Java** and **JavaFX**. It demonstrates a robust Client-Server architecture utilizing TCP Socket programming to facilitate real-time interaction between two players. The system is designed to handle user authentication, lobby management, real-time game state synchronization, and persistent data storage using MySQL.

This application was developed as part of the Network Programming course at the Posts and Telecommunications Institute of Technology (PTIT).

## System Architecture

The project follows a strict **Client-Server** model:

* **Server Side:**
    * **Core:** Multithreaded Java application responsible for handling concurrent client connections.
    * **Database:** Connects to a MySQL database via JDBC to store user accounts, match history, and rankings.
    * **Logic:** Manages game rooms, handles matchmaking, calculates scores, and broadcasts game states to connected clients.
    * **Structure:** Follows the MVC (Model-View-Controller) pattern for organized code management.

* **Client Side:**
    * **UI:** Built with **JavaFX** to provide a responsive and interactive graphical user interface.
    * **Communication:** Maintains a persistent TCP connection with the server using a dedicated `SocketService` to send requests and listen for asynchronous events.
    * **Functionality:** Handles user login, lobby interaction, and the gameplay interface (selecting shot/save direction).

## Technologies Used

* **Programming Language:** Java (JDK 17+)
* **GUI Framework:** JavaFX
* **Networking:** Java Sockets (java.net.Socket, java.net.ServerSocket)
* **Database:** MySQL
* **Build Tool:** Ant (NetBeans Project structure) / Compatible with Maven structures if migrated.
* **IDE:** NetBeans / IntelliJ IDEA / VS Code

## Game Logic & Rules

1.  **Matchmaking:** Two players are paired in a game room. One takes the role of the Striker, and the other plays as the Goalkeeper. Roles switch after each turn.
2.  **Gameplay Loop:**
    * The goal is divided into **6 selectable zones**.
    * In each turn, both players maximize their decision-making:
        * The **Striker** selects a target zone to shoot.
        * The **Goalkeeper** selects a zone to save.
    * **Scoring:** If the Striker's chosen zone is different from the Goalkeeper's, the Striker scores a goal. If the zones match, the shot is blocked (no goal).
3.  **Winning Condition:**
    * The standard match consists of **5 rounds** (turns) per player.
    * If the score is tied after 5 rounds, the match enters a **Sudden Death** phase until a winner is determined.
4.  **State Management:** The server validates all moves to prevent cheating and synchronizes the score and round status to both clients instantly.

## Project Structure

### Server (`/penaltygame_server`)
* `src/penaltyserver/config`: Database configuration and connection pooling.
* `src/penaltyserver/controller`: Handles logic for Authentication, Lobby, Match, and Ranking.
* `src/penaltyserver/model`: Data Access Objects (DAO) and entity classes (User, Match, PenaltyShot).
* `src/penaltyserver/PenaltyServer.java`: Entry point, initializes the ServerSocket.

### Client (`/penaltygame_client`)
* `src/penaltyclient/view`: FXML files and Java controllers for UI (Login, Lobby, Match).
* `src/penaltyclient/model`: Handles client-side logic, data holding, and the `SocketService` for server communication.
* `src/penaltyclient/Assets`: Contains graphics and sound resources.
* `src/penaltyclient/PenaltyClient.java`: Entry point for the JavaFX application.

## Installation and Setup

### Prerequisites
* Java Development Kit (JDK) 8 or higher (Recommended: JDK 17 or 21).
* MySQL Server.
* NetBeans IDE (recommended for existing project structure) or VS Code with Java Extension Pack.

### Step 1: Database Configuration
1.  Create a MySQL database named `penalty_game` (or check `DBConnection.java` for the expected name).
2.  Import the provided SQL script (if available) or create tables for `users`, `matches`, and `penalty_shots` according to the DAO models.
3.  Open `src/penaltyserver/config/DBConnection.java` in the Server project.
4.  Update the `url`, `user`, and `password` constants to match your local MySQL configuration.

### Step 2: Running the Server
1.  Navigate to the `penaltygame_server` directory.
2.  Compile and run the project.
3.  Ensure the console displays a message indicating the server is listening on the specified port (e.g., `Server is running on port 1234...`).

### Step 3: Running the Client
1.  Navigate to the `penaltygame_client` directory.
2.  Compile and run the project.
3.  Launch a second instance of the Client to simulate the second player.
4.  Log in with different accounts on each client instance.
5.  Join the lobby and start a match.

## Possible Future Improvements

* Implement password hashing (BCrypt/Argon2) for enhanced security.
* Add a reconnection mechanism for network interruptions.
* Migrate build system to Maven/Gradle for better dependency management.
* Deploy the server logic to a cloud instance (AWS/GCP).

---
*Disclaimer: This project is for educational purposes as part of the Network Programming curriculum.*
