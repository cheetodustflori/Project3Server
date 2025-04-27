package network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;


public class Server {

    private ArrayList<String> usernames = new ArrayList<>();
    private Queue<ClientThread> waitingClients = new ConcurrentLinkedQueue<>();
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

//        helper method -- send error message
private void sendErrorMessage(String errorText) {
    try {
        Message errorMsg = new Message("error", errorText, "server", null);
        out.writeObject(errorMsg);
        out.flush();
    } catch (IOException e) {
        System.err.println("Failed to send error message: " + e.getMessage());
    }
}

        // handling message type
        private void handleIncomingMessage(Message msg) {
            if (msg.getType().equals("playerInfo")) {
                playerObject = (Player) msg.getContent();
                String newUsername = playerObject.getUsername();

                System.out.println(playerObject.getUsername() + " has joined the waiting room");
                callback.accept(playerObject.getUsername() + " has joined the waiting room");

                waitingClients.add(this);
                if (waitingClients.size() >= 2) {
                    startGameWithNextTwo();
                }


            }
            else if (msg.getType().equals("move")) {
                GameSession currentSession = null;
                for (GameSession session : sessions) {
                    if (session.player1 == this || session.player2 == this) {
                        currentSession = session;
                        break;
                    }
                }
                if (currentSession == null) {
                    return;
                }

                if (currentSession.currentTurn != this) {
                    sendErrorMessage("Not your turn!");
                    return;
                }

                int[] move = (int[]) msg.getContent();
                int row = move[0];
                int col = move[1];

                Message updateMessage = new Message("updateMove", move, playerObject.getUsername(), null);
                currentSession.sendToBoth(updateMessage);

                // Switch turn

                currentSession.currentTurn = (currentSession.currentTurn == currentSession.player1) ? currentSession.player2 : currentSession.player1;
                callback.accept(currentSession.currentTurn.playerObject.getUsername());

                // 🔥 Tell new current player it's their turn
                try {
                    Message yourTurnMsg = new Message("yourTurn", true, "server", null);
                    callback.accept(yourTurnMsg);
                    currentSession.currentTurn.out.writeObject(yourTurnMsg);
                    currentSession.currentTurn.out.flush();
                } catch (IOException e) {
                    System.err.println("Failed to notify next player for turn: " + e.getMessage());
                }
            }

            // CHECK THIS
            else if (msg.getType().equals("boardUpdate")) {
                for (GameSession session : sessions) {
                    if (session.player1 == this || session.player2 == this) {
                        session.sendToBoth(msg);
                        break;
                    }
                }
            }
            else if (msg.getType().equals("usernameCheck")) {
                String full = msg.getContent().toString();
                String stopAt = " has logged in";

                int index = full.indexOf(stopAt);

                String usernameInput = full.substring(0, index);
                for (String username : usernames) {
                    if (username != null && username.equalsIgnoreCase(usernameInput)) {
                        try {
                            Message errorMsg = new Message("loginError", "Username already taken. Please choose a different name.", "server", null);
                            callback.accept("Error: Duplicate usernames");
                            out.writeObject(errorMsg);
                            out.flush();
                        } catch (Exception e) {
                            System.err.println("Failed to send error message: " + e.getMessage());
                            callback.accept("Failed to send error message: " + e.getMessage());
                        }
                        return;
                    }

                }
                try {
                    callback.accept("Log in success! No duplicate usernames");
                    Message success = new Message("loginSuccess", "Successful Log In", "server", null);
                    out.writeObject(success);
                    out.flush();
                    usernames.add(usernameInput);
                } catch (Exception e) {
                    System.err.println("Failed to send success message: " + e.getMessage());
                    callback.accept("Failed to send success message: " + e.getMessage());
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
            private ClientThread currentTurn;



            GameSession(ClientThread p1, ClientThread p2) {

                callback.accept("GAME SESSION STARTED between " + p1.playerObject.getUsername() + " and " + p2.playerObject.getUsername());

                this.player1 = p1;
                this.player2 = p2;
                this.currentTurn = p1;

                try {
                    Message yourTurnMsg = new Message("yourTurn", true, "server", null);
                    callback.accept(this.player1.playerObject.getUsername() + " " + yourTurnMsg.getType());
                    this.currentTurn.out.writeObject(yourTurnMsg);
                    this.currentTurn.out.flush();

                    Message waitTurnMsg = new Message("yourTurn", false, "server", null);
                    callback.accept(this.player1.playerObject.getUsername() + " " + yourTurnMsg.getType());
                    (this.currentTurn == player1 ? player2 : player1).out.writeObject(waitTurnMsg);
                    (this.currentTurn == player1 ? player2 : player1).out.flush();


                } catch (Exception e){
                    System.err.println("Failed to send turn to both players: " + e.getMessage());
                    callback.accept("Failed to send turn to both players: " + e.getMessage());

                }

            }

            public void sendToBoth(Message coord) {
                try {
                    String senderName = (coord.getSender() != null) ? coord.getSender() : "unknown";
                    callback.accept("Sending move from " + senderName);
                    player1.out.writeObject(coord);
                    player1.out.flush();
                    player2.out.writeObject(coord);
                    player2.out.flush();
                } catch (Exception e) {
                    System.err.println("Failed to send board to both players: " + e.getMessage());
                    callback.accept("Failed to send board to both players: " + e.getMessage());
                }
            }
        }

        private void startGameWithNextTwo() {
            ClientThread player1 = waitingClients.poll();
            ClientThread player2 = waitingClients.poll();
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
//
//                Message yourTurnMessage = new Message("yourTurn", true, "server", null);
//                player1.out.writeObject(yourTurnMessage);
//                player1.out.flush();
//
//
//                Message waitTurnMessage = new Message("yourTurn", false, "server", null);
//                player2.out.writeObject(waitTurnMessage);
//                player2.out.flush();


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

                callback.accept("Client #" + count + " connected.");

                while (true) {
                    try {
                        Object obj = in.readObject();

                        if (obj instanceof Message msg) {
                            handleIncomingMessage(msg);
                        } else {
                            System.out.println("Received unknown object: " + obj.getClass().getName());
                        }
                    } catch (Exception e) {
                        System.err.println("Problem with client #" + count + ": " + e.getMessage());
                        callback.accept("Problem with client #" + count + ": " + e.getMessage());
                        callback.accept("Client #" + count + " : " + this.playerObject.getUsername() + " disconnected or error.");
                        clients.remove(this);
                        usernames.remove(this.playerObject.getUsername());
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("ERROR: " + e.getMessage());
                System.err.println("Failed to set up client #" + count);
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





