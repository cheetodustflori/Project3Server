package network;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {

    private ArrayList<ClientThread> waitingClients = new ArrayList<>();
    private ArrayList<GameSession> sessions = new ArrayList<>();

    public Server() {
        TheServer server = new TheServer();
        server.start();
    }

    private class TheServer extends Thread {
        public void run() {
            try (ServerSocket serverSocket = new ServerSocket(5555)) {
                System.out.println("Server is waiting for clients!");

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    ClientThread client = new ClientThread(clientSocket);
                    client.start();
                }
            } catch (Exception e) {
                System.err.println("Server failed: " + e.getMessage());
            }
        }
    }

    private class GameSession {
        private final ClientThread player1;
        private final ClientThread player2;

        GameSession(ClientThread p1, ClientThread p2) {
            this.player1 = p1;
            this.player2 = p2;
        }

        public void sendToBoth(Message message) {
            try {
                player1.out.writeObject(message);
                player1.out.flush();
                player2.out.writeObject(message);
                player2.out.flush();
            } catch (Exception e) {
                System.err.println("Failed to send to both players: " + e.getMessage());
            }
        }
    }

    private class ClientThread extends Thread {
        private Socket connection;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private Player playerObject;
        private boolean isRegistered = false;

        ClientThread(Socket socket) {
            this.connection = socket;
        }

        public void run() {
            try {
                out = new ObjectOutputStream(connection.getOutputStream());
                in = new ObjectInputStream(connection.getInputStream());
                connection.setTcpNoDelay(true);

                while (true) {
                    Object obj = in.readObject();
                    if (obj instanceof Message msg) {
                        handleIncomingMessage(msg);
                    }
                }
            } catch (Exception e) {
                System.err.println("Client disconnected.");
            }
        }

        private void handleIncomingMessage(Message msg) {
            if (msg.getType().equals("playerInfo")) {
                playerObject = (Player) msg.getContent();
                System.out.println("Registered player: " + playerObject.getUsername());

                synchronized (waitingClients) {
                    waitingClients.add(this);
                    if (waitingClients.size() >= 2) {
                        startGameWithNextTwo();
                    }
                }
            } else if (msg.getType().equals("boardUpdate")) {
                for (GameSession session : sessions) {
                    if (session.player1 == this || session.player2 == this) {
                        session.sendToBoth(msg);
                        break;
                    }
                }
            }
        }
    }

    private void startGameWithNextTwo() {
        ClientThread player1 = waitingClients.remove(0);
        ClientThread player2 = waitingClients.remove(0);
        GameSession session = new GameSession(player1, player2);
        sessions.add(session);

        try {
            ArrayList<Player> players = new ArrayList<>();
            players.add(player1.playerObject);
            players.add(player2.playerObject);

            Message partnerFound = new Message("partnerFound", players, "server", null);
            player1.out.writeObject(partnerFound);
            player1.out.flush();
            player2.out.writeObject(partnerFound);
            player2.out.flush();

            System.out.println("New game session started between " + player1.playerObject.getUsername() + " and " + player2.playerObject.getUsername());
        } catch (Exception e) {
            System.err.println("Failed to notify players: " + e.getMessage());
        }
    }
}
