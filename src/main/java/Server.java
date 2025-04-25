import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.scene.control.ListView;


public class Server{

	int count = 1;
	ArrayList<ClientThread> clients = new ArrayList<ClientThread>();
	TheServer server;


	Server(){

		server = new TheServer();
		server.start();
	}


	public class TheServer extends Thread{

		public void run() {

			try(ServerSocket mysocket = new ServerSocket(5555);){
		    System.out.println("Server is waiting for a client!");


		    while(true) {

				ClientThread c = new ClientThread(mysocket.accept(), count);
				clients.add(c);
				c.start();

				count++;

			    }
			} catch(Exception e) {
					System.err.println("Server did not launch");
				}
			}
		}


		class ClientThread extends Thread{


			Socket connection;
			int count;
			ObjectInputStream in;
			ObjectOutputStream out;
			private String username = "Guest" + count;

			ClientThread(Socket s, int count){
				this.connection = s;
				this.count = count;
			}

			public void updateClients(String message) {
				System.out.println(message);
			}

			public void run(){

				try {
					in = new ObjectInputStream(connection.getInputStream());
					out = new ObjectOutputStream(connection.getOutputStream());
					connection.setTcpNoDelay(true);
				}
				catch(Exception e) {
					System.out.println("Streams not open");
				}

				updateClients("new client on server: client #"+count);

				 while(true) {
					    try {
					    	String data = in.readObject().toString();

							if (data.startsWith("USERNAME:")) {
								username = data.substring(9); // grab the name after "USERNAME:"
								System.out.println("Client #" + count + " set username to: " + username);
							} else {
								System.out.println("[" + username + "] says: " + data);
								updateClients("[" + username + "]: " + data);
							}
						}
					    catch(Exception e) {
							System.err.println("Client #" + count + " disconnected.");
							updateClients("[" + username + "] has left the server.");
							clients.remove(this);
							break;
					    }
					}
				}//end of run


		}//end of client thread
}






