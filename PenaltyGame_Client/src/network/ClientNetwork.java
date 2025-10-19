//package network;
//
//import java.io.*;
//import java.net.Socket;
//import java.util.function.Consumer;
//
//public class ClientNetwork {
//    private Socket socket;
//    private ObjectOutputStream out;
//    private ObjectInputStream in;
//    private Thread listenThread;
//    private Socket socket;
//    private DataInputStream input;
//    private DataOutputStream output;
//    private final String SERVER_ADDRESS = "192.168.196.129"; // hoặc IP server
//    private final int SERVER_PORT = 2206;
//
//    // Kết nối đến server
//    public boolean connect(String playerName) {
//        try {
//            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
//            input = new DataInputStream(socket.getInputStream());
//            output = new DataOutputStream(socket.getOutputStream());
//
//            // Gửi thông tin đăng nhập/khởi tạo
//            output.writeUTF("CONNECT:" + playerName);
//            output.flush();
//
//            String response = input.readUTF();
//            return response.equals("CONNECTED");
//        } catch (IOException e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    // Vào hàng chờ matchmaking
//    public void joinQueue() {
//        try {
//            output.writeUTF("JOIN_QUEUE");
//            output.flush();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    // Callback khi nhận dữ liệu từ server
//    private Consumer<Object> onMessageReceived;
//
//    public ClientNetwork(String host, int port, Consumer<Object> onMessageReceived) throws IOException {
//        this.socket = new Socket(host, port);
//        this.out = new ObjectOutputStream(socket.getOutputStream());
//        this.in = new ObjectInputStream(socket.getInputStream());
//        this.onMessageReceived = onMessageReceived;
//
//        // Luồng lắng nghe server
//        listenThread = new Thread(this::listen);
//        listenThread.start();
//    }
//
//    // Lắng nghe dữ liệu server gửi về
//    private void listen() {
//        try {
//            while (true) {
//                Object msg = in.readObject();
//                if (onMessageReceived != null) {
//                    onMessageReceived.accept(msg);
//                }
//            }
//        } catch (Exception e) {
//            System.out.println("Kết nối server bị ngắt: " + e.getMessage());
//        }
//    }
//
//    // Gửi dữ liệu lên server
//    public void send(Object data) {
//        try {
//            out.writeObject(data);
//            out.flush();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    // Đóng kết nối
//    public void disconnect() {
//        try {
//            if (listenThread != null) listenThread.interrupt();
//            if (socket != null) socket.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//}
