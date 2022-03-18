import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TcpServer {
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started!");

            Socket clientSocket = null;
            while (true) {
                try {
                    clientSocket = serverSocket.accept();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    System.exit(-1);
                }
                System.out.println("Client connected!");

                InputStreamReader input = new InputStreamReader(
                        clientSocket.getInputStream(),
                        StandardCharsets.UTF_8
                );
                OutputStream output = clientSocket.getOutputStream();

                System.out.println("Thread assigned");
                Thread myThread = new ClientHandler(clientSocket, input, output);

                myThread.start();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    private static final int DEFAULT_PORT = 9999;
}