package cn.edu.sustech.cs209.chatting.domain;

import java.io.Serializable;
import java.sql.Timestamp;


/**
 * This is used for the database!!!
 */
public record Message(int messageId, String messageContent, int chatId, int senderId, Timestamp sentTime)
        implements Serializable {
}