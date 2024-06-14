package com.yun.IM.models;

public class Message {
    public String message;
    public String senderId;
    public String receiverId;
    public String timestamp;
    public String message_type;

    @Override
    public String toString() {
        return "Message{" +
                "message='" + message + '\'' +
                ", senderId='" + senderId + '\'' +
                ", receiverId='" + receiverId + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", message_type='" + message_type + '\'' +
                '}';
    }

    public Message() {
    }

    public Message(String message, String senderId, String timestamp) {
        this.message = message;
        this.senderId = senderId;
        this.timestamp = timestamp;
    }

    public Message(String message, String senderId, String receiverId, String timestamp, String message_type) {
        this.message = message;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.timestamp = timestamp;
        this.message_type = message_type;
    }

    public String json() {
        return "{" +
                "message='" + message + '\'' +
                ", senderId='" + senderId + '\'' +
                ", receiverId='" + receiverId + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", message_type='" + message_type + '\'' +
                '}';
    }
}
