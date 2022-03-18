import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TcpServer {
    private static String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // empty extension
        }
        return name.substring(lastIndexOf);
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started!");

            // TODO вот здесь надо делать форки

            Socket clientSocket = null;
            while (true) {
                try {
                    clientSocket = serverSocket.accept();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    System.exit(-1);
                }
                System.out.println("Client connected!");

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                clientSocket.getInputStream(),
                                StandardCharsets.UTF_8
                        )
                );

                List<String> lines = new ArrayList<>();
//                HashMap<String, String> requestHeaders = new HashMap<>();
                boolean connection = true;
                while (connection) {
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
                        break;
                    }
                    if (lines.size() == 0) {
                        System.out.println("Invalid request");
                        System.out.println("Client disconnected!");
                        reader.close();
                        break;
                    }
                    Matcher httpReqMatcher = httpReqPattern.matcher(lines.get(0));
                    if (!httpReqMatcher.find()) {
                        System.out.println("Invalid request");
                        System.out.println("Client disconnected!");
                        reader.close();
                        break;
                    }
                    String method = httpReqMatcher.group(1);
                    String filepath = httpReqMatcher.group(2);
                    if (Objects.equals(filepath, "/")) {
                        filepath = "/index.html";
                    }
                    String version = httpReqMatcher.group(3);

//                    requestHeaders.clear();
//                    for(int i = 1; i < lines.size(); i++) {
//                        Matcher headerMatcher = headerPattern.matcher(lines.get(i));
//                        if (!headerMatcher.find()) {
//                            System.out.println("Invalid request");
//                            System.out.println("Client disconnected!");
//                            reader.close();
//                            break;
//                        }
//                        requestHeaders.put(headerMatcher.group(1), headerMatcher.group(2));
//                    }
//                    if (Objects.equals(version, HTTP1_1) && requestHeaders.get("Host") == null) {
//                        System.out.println("Invalid request");
//                        System.out.println("Client disconnected!");
//                        reader.close();
//                        break;
//                    }

                    OutputStream out = clientSocket.getOutputStream();
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
                                out.write(bytes, 0, bytes.length);
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
                            out.write(bytes, 0, bytes.length);
                            out.flush();

                            if (Objects.equals(method, "GET")) {
                                byte[] buffer = new byte[4*1024];
                                while ((fileBytes=fileInputStream.read(buffer))!=-1){
                                    out.write(buffer, 0, fileBytes);
                                    out.flush();
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
                            out.write(bytes, 0, bytes.length);
                        }
                    }
                    out.close();
                    reader.close();
                    System.out.println("Client disconnected!");
                    connection = false;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    private static final int DEFAULT_PORT = 9999;
    private static final Pattern httpReqPattern = Pattern.compile("^(GET|HEAD|POST|PUT|OPTIONS|DELETE|CONNECT|TRACE|PATCH) (/[\\w./\\-?=%&]*) HTTP/(1\\.[01])$");
    private static final Pattern headerPattern = Pattern.compile("^([\\w-]+): (.*)$");
//    private static final String HTTP1_1 = "1.1";
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private static final String rootDir = "/Users/a19567938/IdeaProjects/hl_static_server/static";
}