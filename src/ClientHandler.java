import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ClientHandler extends Thread
{
    private static final Pattern httpReqPattern = Pattern.compile("^(GET|HEAD|POST|PUT|OPTIONS|DELETE|CONNECT|TRACE|PATCH) (/[\\w./\\-?=%&]*) HTTP/(1\\.[01])$");
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private static final String rootDir = "/Users/a19567938/IdeaProjects/hl_static_server/static";
    
    final InputStreamReader input;
    final OutputStream output;
    final Socket mynewSocket;
    
    // Constructor
    public ClientHandler(Socket mynewSocket, InputStreamReader input, OutputStream output)
    {
        this.mynewSocket = mynewSocket;
        this.input = input;
        this.output = output;
    }

    private static String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // empty extension
        }
        return name.substring(lastIndexOf);
    }

    @Override
    public void run()
    {
        BufferedReader reader = new BufferedReader(input);
        List<String> lines = new ArrayList<>();
        try {
            String prevLn = null;
            String ln = null;
            lines.clear();
            try {
                while (!Objects.equals(prevLn = ln, "") && !Objects.equals(ln = reader.readLine(), "") && ln != null) {
                    lines.add(ln);
                    System.out.println(ln);
                    System.out.flush();
                }
            } catch (Exception e) {
                System.out.println("Reading error");
                System.out.println("Client disconnected!");
                reader.close();
                return;
            }
            if (lines.size() == 0) {
                System.out.println("Invalid request");
                System.out.println("Client disconnected!");
                reader.close();
                return;
            }
            Matcher httpReqMatcher = httpReqPattern.matcher(lines.get(0));
            if (!httpReqMatcher.find()) {
                System.out.println("Invalid request");
                System.out.println("Client disconnected!");
                reader.close();
                return;
            }
            String method = httpReqMatcher.group(1);
            String filepath = httpReqMatcher.group(2);
            if (Objects.equals(filepath, "/")) {
                filepath = "/index.html";
            }
            String version = httpReqMatcher.group(3);

            switch (method) {
                case "GET", "HEAD" -> {
                    int fileBytes = 0;
                    File file = null;
                    FileInputStream fileInputStream = null;
                    try {
                        file = new File(String.format("%s%s", rootDir, filepath));
                        fileInputStream = new FileInputStream(file);
                    } catch (Exception ex) {
                        String statusLine = String.format("HTTP/%s 404 Not Found\r\n", version);
                        String responseHeaders = String.format(
                                "Date: %s\r\nServer: hl_static_server\r\nContent-Length: 0\r\nConnection: close\r\nContent-type: text/html\r\n",
                                dateFormatter.format(new Date())

                        );
                        byte[] bytes = String.format("%s%s\r\n", statusLine, responseHeaders).getBytes();
                        this.output.write(bytes, 0, bytes.length);
                        break;
                    }
                    String extension = getFileExtension(file);
                    String contentType = switch (extension) {
                        case (".html") -> "text/html";
                        case (".css") -> "text/css";
                        case (".js") -> "application/javascript";
                        case (".jpg"), (".jpeg") -> "image/jpeg";
                        case (".png") -> "image/png";
                        case (".gif") -> "image/gif";
                        case (".swf") -> "application/x-shockwave-flash";
                        default -> "text/plain";
                    };

                    String statusLine = String.format("HTTP/%s 200 OK\r\n", version);
                    String responseHeaders = String.format(
                            "Date: %s\r\nServer: hl_static_server\r\nContent-Length: %s\r\nConnection: close\r\nContent-type: %s\r\n",
                            dateFormatter.format(new Date()),
                            file.length(),
                            contentType
                    );
                    byte[] bytes = String.format("%s%s\r\n", statusLine, responseHeaders).getBytes();
                    this.output.write(bytes, 0, bytes.length);
                    this.output.flush();

                    if (Objects.equals(method, "GET")) {
                        byte[] buffer = new byte[4 * 1024];
                        while ((fileBytes = fileInputStream.read(buffer)) != -1) {
                            this.output.write(buffer, 0, fileBytes);
                            this.output.flush();
                        }
                    }
                    fileInputStream.close();
                }
                default -> {
                    String statusLine = String.format("HTTP/%s 405 Method Not Allowed\r\n", version);
                    String responseHeaders = String.format(
                            "Date: %s\r\nServer: hl_static_server\r\nContent-Length: 0\r\nConnection: close\r\nContent-type: text/html\r\nAllow: GET, HEAD\n",
                            dateFormatter.format(new Date())

                    );
                    byte[] bytes = String.format("%s%s\r\n", statusLine, responseHeaders).getBytes();
                    this.output.write(bytes, 0, bytes.length);
                }
            }
            this.output.close();
            reader.close();
            System.out.println("Client disconnected!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}