package penaltyclient.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketService {

    private static Socket socket;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;
    
    public static void connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            System.out.println("Connected to: " + host + ": " + port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static ObjectOutputStream getOutputStream() throws IOException {
        if (out == null) {
            throw new IOException("Socket is not connected or output stream is not initialized.");
        }
        return out;
    }

    public static ObjectInputStream getInputStream() throws IOException {
        if (in == null) {
            throw new IOException("Socket is not connected or input stream is not initialized.");
        }
        return in;
    }

    public static void close() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            in = null;
            out = null;
            socket = null;
        }
    }
}