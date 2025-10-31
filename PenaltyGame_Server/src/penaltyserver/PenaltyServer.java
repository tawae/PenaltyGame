package penaltyserver;

import java.io.*;
import java.net.*;
import penaltyserver.controller.LobbyController;
//import penaltyserver.controller.AuthController;
//import penaltyserver.controller.LobbyController;
import penaltyserver.model.ClientHandler;
import penaltyserver.model.SessionManager;
import penaltyserver.model.User;
import penaltyserver.controller.MatchController;
/**
 *
 * @author This PC
 */
public class PenaltyServer {
    private static final int SERVER_PORT = 12345;
    private static MatchController matchController;
    
    public static void main(String[] args) {
        matchController = new MatchController();
        
        LobbyController.setMatchController(matchController);
        
        try(ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("server is running ... ");
            
            while(true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client socket:" + socket); 

                ClientHandler handler = new ClientHandler(socket, matchController);
                handler.start();
            }

        }catch(IOException e) {
            e.printStackTrace();
        }
    }
}
