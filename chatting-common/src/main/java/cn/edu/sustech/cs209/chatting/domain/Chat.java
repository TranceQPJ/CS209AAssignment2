package cn.edu.sustech.cs209.chatting.domain;

import java.io.Serializable;
import java.sql.Timestamp;

public record Chat(int chatId, Timestamp timestamp) implements Serializable {}