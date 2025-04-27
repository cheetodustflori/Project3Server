package network;

import java.io.Serializable;

public class Message implements Serializable {
    static final long serialVersionUID = 42L;

    private MessageType type;      // Use ENUM
    private Object content;
    private String sender;
    private String receiver;

    // Optional extra fields if you want to support specific game info later
    private int moveRow;
    private int moveCol;

    private int playerId;
    private int gameId;
    private boolean bool;          // for boolean flags like login result
    private String message;        // optional simple message string

    // ===== Constructors =====

    // 1. Basic type-content message (general)
    public Message(MessageType type, Object content) {
        this.type = type;
        this.content = content;
    }

    // 2. Full message with sender/receiver
    public Message(MessageType type, Object content, String sender, String receiver) {
        this.type = type;
        this.content = content;
        this.sender = sender;
        this.receiver = receiver;
    }

    // 3. Simple text message
    public Message(String textMessage) {
        this.type = MessageType.TEXT;
        this.message = textMessage;
    }

    // 4. Game move message
    public Message(int moveRow, int moveCol, int gameId, int playerId) {
        this.type = MessageType.GAMEMOVE;
        this.moveRow = moveRow;
        this.moveCol = moveCol;
        this.gameId = gameId;
        this.playerId = playerId;
    }

    // 5. Player login result
    public Message(boolean result, int playerId) {
        this.type = MessageType.LOGINRESULT;
        this.bool = result;
        this.playerId = playerId;
    }

    // 6. General message with just type
    public Message(MessageType type) {
        this.type = type;
    }

    // ===== Getters =====

    public MessageType getType() {
        return type;
    }

    public Object getContent() {
        return content;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public int getMoveRow() {
        return moveRow;
    }

    public int getMoveCol() {
        return moveCol;
    }

    public int getPlayerId() {
        return playerId;
    }

    public int getGameId() {
        return gameId;
    }

    public boolean getBool() {
        return bool;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        if (message != null) {
            return message;
        } else if (content != null) {
            return content.toString();
        }
        return "Message{" + "type=" + type + '}';
    }
}
