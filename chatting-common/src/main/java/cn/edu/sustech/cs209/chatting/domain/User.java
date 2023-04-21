package cn.edu.sustech.cs209.chatting.domain;

import java.io.Serializable;


public record User(int userId, String username, String password) implements Serializable {
    @Override
    public String toString() {
        return username;
    }
}