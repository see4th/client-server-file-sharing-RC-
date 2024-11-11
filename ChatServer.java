import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatServer {

    private static final int PORT = 12345;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is waiting for a connection on port " + PORT);

            while (true) {
                // Accept incoming client connection
                Socket clientSocket = serverSocket.accept();
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
            Scanner scanner = new Scanner(System.in);

            // Thread for receiving messages from the client
            Thread receiveThread = new Thread(() -> {
                try {
                    while (true) {
                        String message = inputStream.readUTF();
                        if (message.equals("end")) {
                            System.out.println("Client has ended the chat.");
                            break;
                        }
                        System.out.println("Client: " + message);
                        System.out.print(">> ");

                    }
                } catch (IOException e) {
                    System.out.println("Connection closed by the client.");
                }
            });

            // Thread for sending messages to the client
            Thread sendThread = new Thread(() -> {
                try {
                    while (true) {
                        System.out.print(">> ");
                        String reply = scanner.nextLine().trim();

                        if (reply.isEmpty()) {
                            continue;
                        }
                        outputStream.writeUTF(reply);

                        if (reply.equalsIgnoreCase("end")) {
                            System.out.println("Ending chat with client.");
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        scanner.close();
                        outputStream.close();
                        clientSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            // Start both threads
            receiveThread.start();
            sendThread.start();

            // Wait for both threads to complete
            receiveThread.join();
            sendThread.join();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
