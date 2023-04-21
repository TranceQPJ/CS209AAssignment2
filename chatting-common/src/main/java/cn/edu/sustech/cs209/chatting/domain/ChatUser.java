package cn.edu.sustech.cs209.chatting.domain;

import java.io.Serializable;

public record ChatUser(int chatId, int userId) implements Serializable {}