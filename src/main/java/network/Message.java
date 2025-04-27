package network;

import java.io.Serializable;

public class Message implements Serializable {
    static final long serialVersionUID = 42L;

    String message;

    public Message(String input){
        message = input;
    }

    public Message(String type, Object content, String sender, String receiver) {
        this.type = type;
        this.content = content;
        this.sender = sender;
        this.receiver = receiver;
    }
    public String toString(){
        return message;
    }

    private String type;         // "move", "chat", "boardUpdate"
    private Object content;
    private String sender;
    private String receiver;

    // Getters
    public String getType() {
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
}
