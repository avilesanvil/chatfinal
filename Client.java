/*

	Richard Delforge, Cameron Devenport, Johnny Do
	Chat Room Project
	COSC 4333 - Distributed Systems
	Dr. Sun
	11/27/2023
	
*/

// Importing necessary Java libraries for input-output operations, networking, and atomic data handling.
import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

// Client class to handle chat client operations.
public class Client {

    // Main method - entry point of the client application.
    public static void main(String[] args) {
        // Scanner to read input from the command line.
        Scanner scanner = new Scanner(System.in);

        // Prompting the user to enter the server IP address and reading the input.
        System.out.print("Enter server IP address (default localhost): ");
        String serverIpInput = scanner.nextLine();
        // Using 'localhost' as default if no input is provided.
        final String serverIp = serverIpInput.isEmpty() ? "localhost" : serverIpInput;

        // Prompting the user to enter the server port number and reading the input.
        System.out.print("Enter server port number (default 9025): ");
        String portInput = scanner.nextLine();
        // Using '9025' as default if no input is provided.
        int mainServerPort = portInput.isEmpty() ? 9025 : Integer.parseInt(portInput);

        // Establishing a connection to the server.
        try (Socket mainServerSocket = new Socket(serverIp, mainServerPort);
             // Setting up output and input streams for communication with the server.
             PrintWriter mainServerOut = new PrintWriter(mainServerSocket.getOutputStream(), true);
             BufferedReader mainServerIn = new BufferedReader(new InputStreamReader(mainServerSocket.getInputStream()));
             // BufferedReader for reading input from the command line.
             BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))) {

            // Printing a message to confirm connection to the server.
            System.out.println("Connected to main server on " + serverIp + ":" + mainServerPort);

            // Atomic integer to store the chat room port number.
            AtomicInteger chatRoomPort = new AtomicInteger();
            // Atomic boolean to track the exit condition of the client.
            AtomicBoolean exitFlag = new AtomicBoolean(false);

            // Starting a new thread to handle server messages.
            new Thread(() -> {
                try {
                    // Variable to store messages received from the server.
                    String serverMessage;
                    // Reading messages from the server until the exit flag is set.
                    while ((serverMessage = mainServerIn.readLine()) != null && !exitFlag.get()) {
                        // Handling messages that indicate the port number of a chat room.
                        if (serverMessage.startsWith("ROOM_PORT:")) {
                            // Extracting the chat room port number from the message.
                            chatRoomPort.set(Integer.parseInt(serverMessage.split(":")[1]));

                            // Establishing a new connection to the chat room.
                            try (Socket chatRoomSocket = new Socket(serverIp, chatRoomPort.get());
                                 PrintWriter chatOut = new PrintWriter(chatRoomSocket.getOutputStream(), true);
                                 BufferedReader chatIn = new BufferedReader(new InputStreamReader(chatRoomSocket.getInputStream()))) {
                                // Implement the logic to handle chat room communication...
                            } catch (IOException e) {
                                // Printing an error message if there is an issue connecting to the chat room.
                                System.err.println("Error connecting to chat room: " + e.getMessage());
                            }
                            break; // Exiting the chat room communication loop.
                        }
                        // Printing the server message to the client's console.
                        System.out.println(serverMessage);
                    }
                } catch (IOException e) {
                    // Printing an error message if there is an issue reading from the server.
                    System.err.println("Error reading from main server: " + e.getMessage());
                }
            }).start(); // Starting the thread.

            // Variable to store user input from the command line.
            String userInput;
            // Continuously reading user input until the exit flag is set.
            while ((userInput = stdIn.readLine()) != null && !exitFlag.get()) {
                // Sending user input to the main server.
                mainServerOut.println(userInput);
                // Setting the exit flag if the user inputs 'EXIT'.
                if ("/exit".equalsIgnoreCase(userInput.trim())) {
                    exitFlag.set(true);
                    break; // Exiting the main client loop.
                }
            }

        } catch (UnknownHostException ex) {
            // Printing an error message if the host is unknown.
            System.err.println("Host unknown: " + ex.getMessage());
        } catch (IOException ex) {
            // Printing an error message if there is an I/O error.
            System.err.println("I/O error: " + ex.getMessage());
        } finally {
            // Printing a message when the client exits.
            System.out.println("Client exited.");
        }
    }
}