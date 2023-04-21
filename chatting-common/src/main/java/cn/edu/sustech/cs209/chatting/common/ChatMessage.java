package cn.edu.sustech.cs209.chatting.common;

import cn.edu.sustech.cs209.chatting.domain.User;

import java.io.Serial;
import java.util.Objects;

public class ChatMessage implements Information {
    @Serial
    private static final long serialVersionUID = 0L;
    private Long timestamp;
    private User sentBy;
    private Integer chatId;
    private String data;


    public ChatMessage(Long timestamp, User sentBy, Integer chatId, String data) {
        this.timestamp = timestamp;
        this.sentBy = sentBy;
        this.chatId = chatId;
        this.data = data;
    }

    public Long timestamp() {
        return timestamp;
    }

    public User sentBy() {
        return sentBy;
    }

    public Integer chatId() {
        return chatId;
    }

    public String data() {
        return data;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ChatMessage) obj;
        return Objects.equals(this.timestamp, that.timestamp) &&
                Objects.equals(this.sentBy, that.sentBy) &&
                Objects.equals(this.chatId, that.chatId) &&
                Objects.equals(this.data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, sentBy, chatId, data);
    }

    @Override
    public String toString() {
        return "ChatMessage[" +
                "timestamp=" + timestamp + ", " +
                "sentBy=" + sentBy + ", " +
                "chatId=" + chatId + ", " +
                "data=" + data + ']';
    }


}
