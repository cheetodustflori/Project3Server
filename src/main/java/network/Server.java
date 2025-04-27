package network;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.Consumer;

public class Server {

    private ArrayList<ClientThread> waitingClients = new ArrayList<>();
    private ArrayList<ClientThread> clients = new ArrayList<>();
    private ArrayList<ClientThread.GameSession> sessions = new ArrayList<ClientThread.GameSession>();
    private Consumer<Serializable> callback;
    int count = 1;
    TheServer server;

    public Server(Consumer<Serializable> call) {

        callback = call;
        server = new TheServer();
        server.start();
    }
//
//    public Server() {
//        TheServer server = new TheServer();
//        server.start();
//    }
//
//    private Consumer<String> guiMessage;
//
//    public Server(Consumer<String> guiCallback) {
//        this.guiMessage = guiCallback; // 👈 Save the reference
//        TheServer server = new TheServer();
//        server.start();
//    }
//
//    private void logToGUI(String message) {
//        if (guiMessage != null) {
//            guiMessage.accept(message);
//        }
//        System.out.println(message); // Optional: still print to console
//    }

    private class TheServer extends Thread {
        public void run() {
            try (ServerSocket serverSocket = new ServerSocket(5555)) {
                System.out.println("Server is waiting for clients!");
                callback.accept("Server is waiting for clients!");


                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    ClientThread c = new ClientThread(clientSocket, count);
                    waitingClients.add(c);
                    clients.add(c);
                    c.start();
                    count++;
                }
            } catch (Exception e) {
                System.err.println("Server failed: " + e.getMessage());
                callback.accept("Server failed: " + e.getMessage());
            }
        }
    }


    private class ClientThread extends Thread {
        private Socket connection;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private Player playerObject;
        int count;

        ClientThread(Socket socket, int count) {
            this.connection = socket;
            this.count = count;
        }

        //        update all clients or just paired players hmmm
        public void updateClients(String message) {
            for (int i = 0; i < clients.size(); i++) {
                ClientThread t = clients.get(i);
                try {
                    t.out.writeObject(message);
                } catch (Exception e) {
                }
            }
        }

        // handling message type
        private void handleIncomingMessage(Message msg) {
            if (msg.getType().equals("playerInfo")) {
                playerObject = (Player) msg.getContent();
                System.out.println(playerObject.getUsername() + " has joined the waiting room");
                callback.accept( playerObject.getUsername() + " has joined the waiting room");

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
            } else if (msg.getType().equals("clientUpdate")) {
                callback.accept(msg.getContent().toString());
            }

//            catch (Exception e) {
//                System.err.println("Client" + count + "disconnected.");
//                callback.accept("Client" + count + "disconnected.");
//                clients.remove(this);
//                break;
//            }
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
                    callback.accept("Failed to send to both players: " + e.getMessage());
                }
            }
        }

        private void startGameWithNextTwo() {
            ClientThread player1 = waitingClients.remove(0);
            ClientThread player2 = waitingClients.remove(1);
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
                callback.accept("New game session started between " + player1.playerObject.getUsername() + " and " + player2.playerObject.getUsername());

            } catch (Exception e) {
                System.err.println("Failed to notify players: " + e.getMessage());
            }
        }

//        RUN

        public void run() {
            try {
                out = new ObjectOutputStream(connection.getOutputStream());
                in = new ObjectInputStream(connection.getInputStream());
                connection.setTcpNoDelay(true);
            } catch (Exception e) {
                System.out.println("Streams not open");
                callback.accept("Streams not open");
            }

//            updateClients("new client on server: client #" + count);
//            callback.accept("new client on server: client #" + count);

            while (true) {
                try {
                    Object obj = in.readObject();

                    if (obj instanceof Message msg) {
                        handleIncomingMessage(msg);
                    } else if (obj instanceof Player player) {
                        // Handle Player object
                        this.playerObject = player;
                        System.out.println("Received Player info: " + player.getUsername());
                        callback.accept("Received Player info: " + player.getUsername());

                        synchronized (waitingClients) {
                            waitingClients.add(this);
                            if (waitingClients.size() >= 2) {
                                startGameWithNextTwo();
                            }
                        }
                    } else {
                        System.out.println("Received unknown object type: " + obj.getClass().getName());
                    }
                } catch (Exception e) {
                    System.err.println(playerObject.getUsername() + " disconnected or error occurred: " + e.getMessage());
                    callback.accept(playerObject.getUsername() + " disconnected or error occurred.");
                    clients.remove(this);
                    break;
                }
            }


        }


//            while (true) {
//                try {
//                    String data = in.readObject().toString();
//                    callback.accept("client: " + count + " sent: " + data);
//                    updateClients("client #" + count + " said: " + data);
//
//                } catch (Exception e) {
//                    callback.accept("OOOOPPs...Something wrong with the socket from client: " + count + "....closing down!");
//                    updateClients("Client #" + count + " has left the server!");
//                    clients.remove(this);
//                    break;
//                }
//            }
    }//end of run

}





