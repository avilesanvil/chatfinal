/*

	Richard Delforge, Cameron Devenport, Johnny Do
	Chat Room Project
	COSC 4333 - Distributed Systems
	Dr. Sun
	11/27/2023
	
*/

// Importing necessary Java libraries for networking, input-output operations, and concurrency.
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

// Server class to handle chat room operations and client connections.
public class Server {
    // Default port number for the server.
    private static final int DEFAULT_PORT = 9025;
    // Maximum valid port number for network communication.
    private static final int MAX_PORT = 65535;
    // ConcurrentHashMap to store active chat rooms, allowing thread-safe operations.
    private static Map<String, ChatRoomHandler> chatRooms = new ConcurrentHashMap<>();

    // Main method - entry point of the server application.
    public static void main(String[] args) {

        // Variables for IP address and port, initialized with default values.
        String ipAddress = "0.0.0.0"; // Default IP address to listen on all interfaces.
        int port = DEFAULT_PORT;        // Default port for the server.

        // Checking command-line arguments for custom IP address and port.
        if (args.length > 0) {
            ipAddress = args[0]; // Get IP address from the first command-line argument.
            if (args.length > 1) {
                port = Integer.parseInt(args[1]); // Get port from the second command-line argument, if provided.
            }
        }

        // Creating a thread pool with a fixed number of threads for handling client requests.
        ExecutorService pool = Executors.newFixedThreadPool(10);

        try (ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByName(ipAddress))) {
            // Display the port the server is listening on
            System.out.println("Server on Port: " + serverSocket.getLocalPort());

            // Server's main loop to accept client connections.
            while (true) {
                // Accepting a connection from a client.
                Socket clientSocket = serverSocket.accept();
                // Logging the IP address of the connected client.
                System.out.println("Client connected from " + clientSocket.getInetAddress().getHostAddress());
                // Assigning a new task (client handling) to the thread pool.
                pool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException ex) {
            // Logging server exceptions.
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // Method to find an available port starting from a given port number.
    private static int findAvailablePort(int startPort) {
        // Iterating over port numbers starting from the given port.
        while (startPort <= MAX_PORT) {
            try (ServerSocket serverSocket = new ServerSocket(startPort)) {
                // If a ServerSocket is successfully created, the port is available.
                return startPort;
            } catch (IOException ignored) {
                // If the port is already in use, increment the port number and try again.
                startPort++;
            }
        }
        // Return -1 if no available port is found.
        return -1;
    }
    
    // Nested class for handling individual chat rooms.
    private static class ChatRoomHandler implements Runnable {
        // Name of the chat room.
        private String roomName;
        // Set of clients (PrintWriters) in the chat room, allowing concurrent access.
        private Set<PrintWriter> clients = ConcurrentHashMap.newKeySet();
        // ServerSocket for the chat room.
        private ServerSocket serverSocket;
        // Set of client sockets connected to the chat room.
        private Set<Socket> clientSockets = ConcurrentHashMap.newKeySet();
        // Lock object for synchronizing access to the port usage.
        private static final Object portLock = new Object();
        // Set to keep track of used ports.
        private static Set<Integer> usedPorts = new HashSet<>();

        // Static method to log port assignment and release.
        public static void logPortUsage(int port, boolean assigned) {
            synchronized (portLock) {
                if (assigned) {
                    // Add the port to the set of used ports.
                    usedPorts.add(port);
                    System.out.println("Assigned port: " + port);
                } else {
                    // Remove the port from the set of used ports.
                    usedPorts.remove(port);
                    System.out.println("Released port: " + port);
                }
            }
        }

        // Constructor for ChatRoomHandler, setting up a new chat room on a given port.
        public ChatRoomHandler(String roomName, int port) throws IOException {
            this.roomName = roomName;
            this.serverSocket = new ServerSocket(port);
            // Log the assignment of a new port for this chat room.
            logPortUsage(port, true);
        }

        // Getter method for the chat room's port.
        public int getPort() {
            return serverSocket.getLocalPort();
        }

        // Method to add a client (PrintWriter) to the chat room.
        public void addClient(PrintWriter client) {
            clients.add(client);
        }

        // Method to remove a client from the chat room.
        public void removeClient(PrintWriter client) {
            clients.remove(client);
            // If the chat room becomes empty, remove it from the map of chat rooms.
            if (clients.isEmpty()) {
                chatRooms.remove(roomName);
            }
        }

        // Method to broadcast a message to all clients in the chat room.
        public void broadcastMessage(String message) {
            for (PrintWriter client : clients) {
                client.println(message);
            }
        }

        // Method to get the number of clients in the chat room.
        public int getNumberOfClients() {
            return clients.size();
        }

        // Run method for the chat room's thread.
        public void run() {
            // Keep the chat room open until the server socket is closed.
            while (!serverSocket.isClosed()) {
                try {
                    // Accept a new client connection.
                    Socket clientSocket = serverSocket.accept();
                    // Add the client socket to the set of client sockets.
                    clientSockets.add(clientSocket);
                    // Start a new thread to handle individual client communication.
                    new Thread(new IndividualClientHandler(clientSocket)).start();
                } catch (IOException e) {
                    // Handle any exceptions that occur during client connection or communication.
                }
            }
        }
    }

    // Nested class for handling communication with individual clients in a chat room.
    private static class IndividualClientHandler implements Runnable {
        // Client socket for this handler.
        private Socket clientSocket;

        // Constructor to set up the client socket.
        public IndividualClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        // Run method for the client handler's thread.
        public void run() {
            // Implement the logic for handling communication with an individual client.
        }
    }
    
	    // Nested class for handling each client connected to the server.
    private static class ClientHandler implements Runnable {
        private Socket clientSocket; // Socket for communication with the client.
        private PrintWriter out; // Writer to send data to the client.
        private BufferedReader in; // Reader to receive data from the client.
        private String currentRoom; // The name of the chat room the client is currently in.
        private String clientName; // The name of the client.

        // Constructor for the ClientHandler, initializing it with the client's socket.
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        // The run method of the thread, containing the main logic for client interaction.
        public void run() {
            try {
                // Setting up output and input streams for communication with the client.
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                // Asking the client for their name and reading the response.
                out.println("Enter your name:");
                clientName = in.readLine();
                // Sending a welcome message and instructions to the client.
                out.println("Welcome " + clientName + "! You can join a room with /join <room_name>, leave with /leave, list existing chatrooms with /listrooms, exit the server with /exit, or send messages.");

                // Variable to store input from the client.
                String inputLine;
                // Continuously reading messages from the client.
                while ((inputLine = in.readLine()) != null) {
                    // Handling different commands based on the input.
                    if (inputLine.startsWith("/join ")) {
                        // Handling JOIN command to join a chat room.
                        joinChatRoom(inputLine.substring(5));
                    } else if ("/leave".equals(inputLine)) {
                        // Handling LEAVE command to leave the current chat room.
                        leaveChatRoom();
                    } else if ("/listrooms".equals(inputLine)) {
                        // Handling LISTROOMS command to list all active chat rooms.
                        listChatRooms();
                    } else if ("/exit".equals(inputLine)) {
                        // Handling EXIT command to disconnect the client.
                        if(currentRoom != null) {
                            leaveChatRoom();
                        }
                        System.out.println("Client: '" + clientName + "' has disconnected (EXIT command). Time: " + 
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                        out.println("Exiting the server. Goodbye!");
                        closeResources();
                        break; // Exiting the loop and ending the thread.
                    } else {
                        // Sending any other input as a message to the chat room.
                        sendMessageToChatRoom(clientName + ": " + inputLine, this.out);
                    }
                }
                
            } catch (IOException ex) {
                // Handling exceptions related to client communication.
                System.out.println("Server exception: " + ex.getMessage());
                ex.printStackTrace();
            } finally {
                // Finally block to ensure the client is properly disconnected.
                if (currentRoom != null) {
                    leaveChatRoom();  // Ensuring the client leaves the chat room if still connected.
                }
                out.println("SERVER_CLOSE_CONNECTION"); // Informing the client of server-initiated disconnection.
                closeResources();  // Closing all open resources for this client.
            }
        }

        // Method to handle client's request to join a chat room.
        private void joinChatRoom(String roomName) {
            // Retrieving or creating a chat room with the specified name.
            ChatRoomHandler roomHandler = chatRooms.computeIfAbsent(roomName, k -> {
                try {
                    // Finding an available port for the new chat room.
                    int newRoomPort = findAvailablePort(DEFAULT_PORT);
                    return new ChatRoomHandler(roomName, newRoomPort);
                } catch (IOException e) {
                    throw new RuntimeException("Error creating chat room", e);
                }
            });

            // Determining if the chat room is new.
            boolean isNewRoom = !chatRooms.containsKey(roomName) || roomHandler.getNumberOfClients() == 0;
            leaveChatRoom(); // Leaving the current chat room, if any.
            // Adding the client to the new chat room.
            roomHandler.addClient(out);
            currentRoom = roomName;
            // Starting a new thread for the chat room if it's new.
            if (isNewRoom) {
                Thread newRoomThread = new Thread(roomHandler);
                newRoomThread.start(); // Starting the chat room handler thread.
                System.out.println("New thread created for chat room: " + roomName + ", Thread ID: " + newRoomThread);
            }

            // Notifying the client of successful join and the port number of the chat room.
            out.println("You have successfully joined the room: " + roomName);
            out.println("NOTICE: Room Server PORT: " + roomHandler.getPort());
        }

        // Method to handle client's request to leave the current chat room.
        private void leaveChatRoom() {
            if (currentRoom != null) {
                // Retrieving the chat room handler for the current room.
                ChatRoomHandler roomHandler = chatRooms.get(currentRoom);
                if (roomHandler != null) {
                    // Removing the client from the chat room.
                    roomHandler.removeClient(out);
                    out.println("Left room: " + currentRoom);
                    System.out.println(clientName + " has left chat room: " + currentRoom);

                    // Checking if the chat room is empty after the client leaves.
                    if (roomHandler.getNumberOfClients() == 0) {
                        // Releasing the port if the chat room is empty.
                        roomHandler.logPortUsage(roomHandler.getPort(), false); // Logging released port.
                        chatRooms.remove(currentRoom); // Removing the empty chat room from the map.
                    }
                }
                // Setting the current room to null as the client has left.
                currentRoom = null;
            }
        }

        // Method to list all active chat rooms to the client.
        private void listChatRooms() {
            for (Map.Entry<String, ChatRoomHandler> entry : chatRooms.entrySet()) {
                String roomName = entry.getKey();
                ChatRoomHandler roomHandler = entry.getValue();
                int numberOfUsers = roomHandler.getNumberOfClients(); // Number of clients in the chat room.
                int roomPort = roomHandler.getPort(); // Port number of the chat room.
                out.println(" - " + roomName + " (" + numberOfUsers + " users) - PORT: " + roomPort);
            }
        }

        // Method to send a message to the chat room.
        private void sendMessageToChatRoom(String message, PrintWriter senderOut) {
            // Formatting the message with a timestamp.
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            String formattedMessage = "\n" + "[" + time + "] " + message.trim();
            if (currentRoom != null) {
                // Retrieving the chat room handler for the current room.
                ChatRoomHandler roomHandler = chatRooms.get(currentRoom);
                if (roomHandler != null) {
                    // Broadcasting the message to all clients in the chat room.
                    roomHandler.broadcastMessage(formattedMessage);
                }
            }                
        }

        // Method to close all resources associated with this client.
        private void closeResources() {
            try {
                // Closing the PrintWriter, BufferedReader, and Socket.
                if (out != null) out.close();
                if (in != null) in.close();
                if (clientSocket != null) clientSocket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
