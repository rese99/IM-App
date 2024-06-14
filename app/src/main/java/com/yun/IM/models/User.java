package com.yun.IM.models;

import java.io.Serializable;

public class User implements Serializable {
    public String name;
    public String image;
    public String email;
    public String id;
    public String token;
    public String AES;
    public String password;

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", image='" + image + '\'' +
                ", email='" + email + '\'' +
                ", id='" + id + '\'' +
                '}';
    }

    public User() {
    }

    public User(String name, String image, String email, String id) {
        this.name = name;
        this.image = image;
        this.email = email;
        this.id = id;
    }

    public User(String name, String image, String email, String id, String AES) {
        this.name = name;
        this.image = image;
        this.email = email;
        this.id = id;
        this.AES = AES;
    }
}
