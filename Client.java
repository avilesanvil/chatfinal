import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;


public class Client {

    public static void main(String[] args) {
    Scanner scanner = new Scanner(System.in);

    System.out.print("Enter server IP address (default localhost): ");
    String serverIpInput = scanner.nextLine();
    final String serverIp = serverIpInput.isEmpty() ? "localhost" : serverIpInput;

    System.out.print("Enter server port number (default 9025): ");
    String portInput = scanner.nextLine();
    int mainServerPort = portInput.isEmpty() ? 9025 : Integer.parseInt(portInput);

    try (Socket mainServerSocket = new Socket(serverIp, mainServerPort);
         PrintWriter mainServerOut = new PrintWriter(mainServerSocket.getOutputStream(), true);
         BufferedReader mainServerIn = new BufferedReader(new InputStreamReader(mainServerSocket.getInputStream()));
         BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))) {

        System.out.println("Connected to main server on " + serverIp + ":" + mainServerPort);

        AtomicInteger chatRoomPort = new AtomicInteger();
        AtomicBoolean exitFlag = new AtomicBoolean(false);

        new Thread(() -> {
            try {
                String serverMessage;
                while ((serverMessage = mainServerIn.readLine()) != null && !exitFlag.get()) {
                    if (serverMessage.startsWith("ROOM_PORT:")) {
                        chatRoomPort.set(Integer.parseInt(serverMessage.split(":")[1]));

                        try (Socket chatRoomSocket = new Socket(serverIp, chatRoomPort.get());
                             PrintWriter chatOut = new PrintWriter(chatRoomSocket.getOutputStream(), true);
                             BufferedReader chatIn = new BufferedReader(new InputStreamReader(chatRoomSocket.getInputStream()))) {
                            // Handle chat room communication...
                        } catch (IOException e) {
                            System.err.println("Error connecting to chat room: " + e.getMessage());
                        }
                        break; // Exit the chat room communication loop
                    }
                    System.out.println(serverMessage);
                }
            } catch (IOException e) {
                System.err.println("Error reading from main server: " + e.getMessage());
            }
        }).start();

        String userInput;
        while ((userInput = stdIn.readLine()) != null && !exitFlag.get()) {
            mainServerOut.println(userInput);
            if ("EXIT".equalsIgnoreCase(userInput.trim())) {
                exitFlag.set(true);
                break; // Exit the main client loop
            }
        }

		} catch (UnknownHostException ex) {
			System.err.println("Host unknown: " + ex.getMessage());
		} catch (IOException ex) {
			System.err.println("I/O error: " + ex.getMessage());
		} finally {
			System.out.println("Client exited.");
		}
	}

}



