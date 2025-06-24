import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    static final int MaxPlayers = 4;
    private static ArrayList<ClientHandler> ConnectedPlayers = new ArrayList<>();
    public static ArrayList<ClientHandler> WaitingRoom = new ArrayList<>();
    public static boolean gameStarted = false;

    public static void main(String[] args) throws IOException {
        try {
            ServerSocket serverSocket = new ServerSocket(5555);
            System.out.println("Waiting for connection...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler client = new ClientHandler(clientSocket);

                // Always accept new players in ConnectedPlayers list
                ConnectedPlayers.add(client);
                new Thread(client).start();
                broadcastPlayersList();

                // If the waiting room is full, disable the Play button for new players as well
                if (WaitingRoom.size() >= MaxPlayers) {
                    client.sendMessage("WAITING_ROOM_FULL");
                }
            }
        } catch (IOException e) {
            System.err.println("IO Exception");
        }
    }


    public static synchronized void broadcastPlayersList() {
        for (ClientHandler client : ConnectedPlayers) {
            StringBuilder PlayersList = new StringBuilder("Players connected: ");
            for (ClientHandler player : ConnectedPlayers) {
                PlayersList.append(player.getPlayerName()).append(", ");
            }
            client.sendMessage(PlayersList.toString());
        }
    }

    public static synchronized void broadcastWaitingRoom() {
        for (ClientHandler client : WaitingRoom) {
            StringBuilder waitingRoomStatus = new StringBuilder("Waiting Room: ");
            for (ClientHandler player : WaitingRoom) {
                waitingRoomStatus.append(player.getPlayerName()).append(", ");
            }
            client.sendMessage(waitingRoomStatus.toString());
        }
    }

    public static synchronized void addToWaitingRoom(ClientHandler c) {
        if (gameStarted) {
            c.sendMessage("WAITING_ROOM_CLOSED"); // Notify that the waiting room is closed
            return;
        }

        if (WaitingRoom.size() >= MaxPlayers) {
            c.sendMessage("WAITING_ROOM_FULL"); // Notify client they cannot join the waiting room
            return;
        }

        if (!WaitingRoom.contains(c)) {
            WaitingRoom.add(c);
            ConnectedPlayers.remove(c);
            c.sendMessage("WAITING_ROOM");
            broadcastPlayersList();
            broadcastWaitingRoom();

            if (WaitingRoom.size() == MaxPlayers) {
                // Notify ALL clients to disable the Play button
                for (ClientHandler client : ConnectedPlayers) {
                    client.sendMessage("WAITING_ROOM_FULL");
                }
                for (ClientHandler client : WaitingRoom) {
                    client.sendMessage("START_BUTTON_ENABLED"); // Enable Start Game button
                }
            }
        }
    }




    public static synchronized void startGame() {
        gameStarted = true;
        broadcastToWaitingRoom("GAME_STARTED");
        broadcastToWaitingRoom("START_BUTTON_DISABLED"); // Disable "Start Game" for all players
    }


    public static synchronized void broadcastToWaitingRoom(String msg) {
        for (ClientHandler client : WaitingRoom) {
            client.sendMessage(msg);
        }
    }

    public static synchronized void removePlayer(ClientHandler client) {
        ConnectedPlayers.remove(client);
        WaitingRoom.remove(client);
        broadcastPlayersList();
        broadcastWaitingRoom();
    }
}

class ClientHandler implements Runnable {
    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private String playerName;

    public ClientHandler(Socket clientSocket) throws IOException {
        this.client = clientSocket;
        this.in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        this.out = new PrintWriter(client.getOutputStream(), true);
    }

    public void run() {
        try {
            playerName = in.readLine();

            Server.broadcastPlayersList(); // Always update players list

            String input;
            while ((input = in.readLine()) != null) {
                if (input.equalsIgnoreCase("play")) {
                    Server.addToWaitingRoom(this);
                } else if (input.equalsIgnoreCase("start")) {
                    if (Server.WaitingRoom.size() >= Server.MaxPlayers) {
                        Server.startGame();
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Client " + playerName + " disconnected.");
        } finally {
            Server.removePlayer(this);
            closeConnection();
        }
    }

    public String getPlayerName() {
        return playerName;
    }

    public void sendMessage(String msg) {
        out.println(msg);
    }

    public void closeConnection() {
        try {
            in.close();
            out.close();
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
