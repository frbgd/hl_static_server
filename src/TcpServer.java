import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TcpServer {
    public static void main(String[] args) {

        HashMap<String, String> config = new HashMap<>();
        try(FileReader fr = new FileReader(DEFAULT_CONFIG_PATH))
        {
            BufferedReader reader = new BufferedReader(fr);
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher configMatcher = configPattern.matcher(line);
                if (!configMatcher.find()) {
                    System.out.println("Invalid config!");
                    return;
                }
                config.put(configMatcher.group(1), configMatcher.group(2));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try (ServerSocket serverSocket = new ServerSocket(DEFAULT_PORT)) {
            System.out.println("Server started!");

            Socket clientSocket = null;
            while (true) {
                try {
                    clientSocket = serverSocket.accept();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    System.exit(-1);
                }

                InputStreamReader input = new InputStreamReader(
                        clientSocket.getInputStream(),
                        StandardCharsets.UTF_8
                );
                OutputStream output = clientSocket.getOutputStream();

                Thread myThread = new ClientHandler(
                        config.get("document_root"),
                        clientSocket,
                        input,
                        output
                );

                myThread.start();

            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    private static final int DEFAULT_PORT = 81;
    private static final String DEFAULT_CONFIG_PATH = "/etc/httpd.conf";
    private static final Pattern configPattern = Pattern.compile("^(\\w+) (.+)$");
}