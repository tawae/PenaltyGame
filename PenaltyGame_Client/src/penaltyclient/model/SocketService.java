/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package penaltyclient.model;

import java.io.*;
import java.net.*;

/**
 *
 * @author This PC
 */
public class SocketService {
    private static Socket socket;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;

    
    public static void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }
    
    public static ObjectOutputStream getOutputStream() {
        return out;
    }

    public static ObjectInputStream getInputStream() {
        return in;
    }

    public static void close() throws IOException {
        socket.close();
    }
}