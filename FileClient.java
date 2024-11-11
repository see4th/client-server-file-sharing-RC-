import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class FileClient {
    public static void main(String[] args) {
        String serverAddress = "localhost"; // or the server's IP address
        int port = 12345;

        try (Socket socket = new Socket(serverAddress, port)) {
            // Set up input/output streams
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            Scanner scanner = new Scanner(System.in);

            System.out.print("Enter your client ID: ");
            String clientId = scanner.nextLine();
            outputStream.writeUTF(clientId); // Send client ID to the server

            System.out.println("Choose an operation: ");
            System.out.println("1. Upload to server");
            System.out.println("2. Download form server");
            System.out.println("3. Send to other client");
            System.out.println("4. Listen and wait");
            // broadcast *************************
            // chat ********************** add the chat code of the files (ChatClient, ChatServer)

            int choice = scanner.nextInt();
            scanner.nextLine();

            if (choice == 1) {
                System.out.print("Enter the path of the file to send: ");
                String filePath = scanner.nextLine();
                UploadFile(filePath, outputStream);
            } else if (choice == 2) {
                DownloadFile(scanner, socket);
            } else if (choice == 3) {
                // add displaying all other connected clients *************************
                System.out.print("Enter the target client ID: ");
                String targetClientId = scanner.nextLine();

                System.out.print("Enter the path of the file to send: ");
                String filePath = scanner.nextLine();

                sendFile(filePath, targetClientId, outputStream);
            } else if (choice == 4) {
                receiveFile(inputStream, clientId);
            } else {
                System.out.println("Invalid choice");
            }

            scanner.close();
            outputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void UploadFile(String filePath, DataOutputStream outputStream) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                outputStream.writeUTF("SEND-S"); // Tell the server to expect a file upload
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void DownloadFile(Scanner scanner, Socket socket) {
        try {
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());

            outputStream.writeUTF("RECEIVE"); // Tell the server to send a file

            String available_files = inputStream.readUTF();
            if (available_files.equals("0")) {
                System.out.println("no files available.");
                return;
            } else {
                String[] available_files_list = available_files.split(",");
                System.out.println("choose which file to request: ");
                int i = 1;
                for (String file : available_files_list) {
                    System.out.println(i + ". " + file);
                    i++;
                }
                int choice = scanner.nextInt() - 1; // minus 1 to align the indexing
                outputStream.writeUTF(available_files_list[choice]); // Requested file from the server
            }

            // Receive the file
            String receivedFileName = inputStream.readUTF();
            long fileSize = inputStream.readLong();

            LocalTime currentTime = LocalTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HHmmssSSS");
            String formattedTime = currentTime.format(formatter);

            try (FileOutputStream fileOutputStream = new FileOutputStream("received_" +
                    formattedTime + "_" + receivedFileName)) {
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
            }
            System.out.println("File received successfully");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendFile(String filePath, String targetClientId, DataOutputStream outputStream) {

        try {
            File file = new File(filePath);
            if (file.exists()) {
                outputStream.writeUTF("SEND-C"); // Command to send file
                outputStream.writeUTF(targetClientId); // Target client ID
                outputStream.writeUTF(file.getName()); // File name
                outputStream.writeLong(file.length()); // File size

                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
                System.out.println("File sent successfully to " + targetClientId);
            } else {
                System.out.println("File does not exist.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void receiveFile(DataInputStream inputStream, String clientId) {
        try {
            while (true) {
                String command = inputStream.readUTF();
                if (command.equals("RECEIVE")) {
                    String fileName = inputStream.readUTF();
                    long fileSize = inputStream.readLong();

                    try (FileOutputStream fileOutputStream = new FileOutputStream("received_" + clientId + fileName)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        long totalBytesRead = 0;

                        while (totalBytesRead < fileSize && (bytesRead = inputStream.read(buffer)) != -1) {
                            fileOutputStream.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;
                        }
                    }
                    System.out.println("File " + fileName + " received successfully.");
                    // add recieved from *************************
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
