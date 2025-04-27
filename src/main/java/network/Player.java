package network;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

public class Player implements Serializable {
    private static final long serialVersionUID = 1L;
//    private transient Socket socket;
//    private transient ObjectOutputStream outputStream;
//    private transient ObjectInputStream inputStream;

    String username;
    int column;
    private String iconPath;
    String gameStatus;
    Boolean isTurn;

    public Player(String username, int column, String icon, String gameStatus, Boolean isTurn){
//        this.socket = socket;
//        this.outputStream = outputStream;
//        this.inputStream = inputStream;
        this.username = username;
        this.column = column;
        this.iconPath = icon;
        this.gameStatus = gameStatus;
        this.isTurn = isTurn;
    }

//    GETTERS

    public String getUsername(){
        return username;
    }

    public String getIcon() {
        return iconPath;
    }


    public int getMove() {
        return column;
    }

    public String getGameStatus() {
        return gameStatus;
    }

    public boolean getIsTurn() {
        return isTurn;
    }


//    SETTERS

    public void setUsername(String username){
        this.username = username;
    }

    public void setMove(int column){
        this.column = column;
    }

    public void setGameStatus(String gameStatus){
        // waiting, playing, win, tie, lose
        this.gameStatus = gameStatus;
    }

    public void setIsTurn(Boolean isTurn){
        this.isTurn = isTurn;
    }

    public void setIcon(String icon){
        this.iconPath = icon;
    }

// UPDATE SERVER

//    public void sendPlayer() {
//        try {
//            Message playerMessage = new Message("playerInfo", this, username, null);
//            outputStream.writeObject(playerMessage);
//            outputStream.flush();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

//    public void sendMessage(Message message){
//        try {
//            outputStream.writeObject(message);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

}
