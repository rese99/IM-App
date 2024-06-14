package com.yun.IM.models;

public class Friends {
    public String userId;
    public String name;
    public String image;
    public String email;
    public String id;
    public String lastMessage;
    public long dateObject;
    public String studs;
    public String friend_message;
    public String message_type;
    public String aes;

    public Friends() {
    }

    public Friends(String userId, String id, String lastMessage, long dateObject,String message_type) {
        this.userId = userId;
        this.id = id;
        this.lastMessage = lastMessage;
        this.dateObject = dateObject;
        this.message_type=message_type;
    }

    @Override
    public String toString() {
        return "Friends{" +
                "userId='" + userId + '\'' +
                ", name='" + name + '\'' +
                ", image='" + image + '\'' +
                ", email='" + email + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
