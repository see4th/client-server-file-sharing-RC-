import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

public class FileServer {

    private static ConcurrentHashMap<String, Socket> clients = new ConcurrentHashMap<>();
    private static final int PORT = 12345;

    public static void main(String[] args) {

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is waiting for a connection on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept(); // Listens for a connection to be made to this socket and
                                                             // accepts it. The method blocks until a connection is
                                                             // made.
                System.out.println("Client connected");

                // Handle client requests in a new thread
                new Thread(() -> handleClientRequest(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClientRequest(Socket clientSocket) {
        try {
            DataInputStream inputStream = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream outputStream = new DataOutputStream(clientSocket.getOutputStream());

            String clientId = inputStream.readUTF(); // First message from client should be their ID
            clients.put(clientId, clientSocket);

            // Receive the request type (send or receive file)
            String requestType = inputStream.readUTF();
            if ("SEND-S".equals(requestType)) {
                receiveFile(inputStream);
            } else if ("RECEIVE".equals(requestType)) {
                sendFile(outputStream, inputStream);
            } else if ("SEND-C".equals(requestType)) {
                sendFileToClient(outputStream, inputStream);
            } else {
                System.out.println("Invalid request type");
            }

            inputStream.close();
            outputStream.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void receiveFile(DataInputStream inputStream) throws IOException {
        String fileName = inputStream.readUTF(); 
        long fileSize = inputStream.readLong(); 

        LocalTime currentTime = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HHmmssSSS");
        String formattedTime = currentTime.format(formatter);

        try (FileOutputStream fileOutputStream = new FileOutputStream("received_" + formattedTime + "_" + fileName)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            long bytesReceived = 0;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
                bytesReceived += bytesRead;
                if (bytesReceived >= fileSize) {
                    break;
                }
            }
            System.out.println("File received successfully");
        }
    }

    private static void sendFile(DataOutputStream outputStream, DataInputStream inputStream) throws IOException {

        File server_files_directory = new File("C:\\Users\\HP\\Desktop\\unvr\\RC\\file sharing\\server_directory");

        File[] files = server_files_directory.listFiles();

        if (files != null) {
            if (files.length == 0) {
                outputStream.writeUTF("0");
                return;
            } else {
                String fileNames = Arrays.stream(files)
                        .map(File::getName) 
                        .collect(Collectors.joining(","));

                outputStream.writeUTF(fileNames);
            }

        } else {
            System.out.println("The specified server directory path is not valid.");
        }

        String fileName = inputStream.readUTF();
        System.out.println("SENDING " + fileName);
        File file = new File("server_directory\\" + fileName);

        if (file.exists()) {
            outputStream.writeUTF(file.getName()); // Send file name
            outputStream.writeLong(file.length()); // Send file size

            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            System.out.println("File sent successfully");
        } else {
            System.out.println("File does not exist");
        }
    }

    private static void sendFileToClient(DataOutputStream outputStream, DataInputStream inputStream) throws IOException{
        String targetClientId = inputStream.readUTF();  // Read target client ID
        String fileName = inputStream.readUTF();
        long fileSize = inputStream.readLong();
    
        Socket targetSocket = clients.get(targetClientId);
        if (targetSocket != null && !targetSocket.isClosed()) {
            DataOutputStream targetOutput = new DataOutputStream(targetSocket.getOutputStream());
            targetOutput.writeUTF("RECEIVE");  // Notify target client of incoming file
            targetOutput.writeUTF(fileName);
            targetOutput.writeLong(fileSize);
    
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytesRead = 0;
            while (totalBytesRead < fileSize && (bytesRead = inputStream.read(buffer)) != -1) {
                targetOutput.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }
            System.out.println("File " + fileName + " sent to " + targetClientId);
        } else {
            System.out.println("Target client " + targetClientId + " not available.");
        }
    }
    
}

