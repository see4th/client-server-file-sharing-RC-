import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    public static void main(String[] args) {
        String serverAddress = "localhost"; // or the server's IP address
        int port = 12345;

        try (Socket socket = new Socket(serverAddress, port)) {
            // Set up input/output streams
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            Scanner scanner = new Scanner(System.in);

            System.out.println("Connected to the server.");

            // Thread for receiving messages from the server
            Thread receiveThread = new Thread(() -> {
                try {
                    while (true) {
                        String message = inputStream.readUTF();
                        if (message.equals("end")) {
                            System.out.println("Server has ended the chat.");
                            break;
                        }
                        System.out.println("Server: " + message);
                        System.out.print(">> ");

                    }
                } catch (IOException e) {
                    System.out.println("Connection closed by the server.");
                }
            });

            // Thread for sending messages to the server
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
                            System.out.println("Ending chat.");
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        scanner.close();
                        outputStream.close();
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            // Start both threads
            receiveThread.start();
            sendThread.start();

            // Wait for both threads to finish before exiting
            receiveThread.join();
            sendThread.join();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
