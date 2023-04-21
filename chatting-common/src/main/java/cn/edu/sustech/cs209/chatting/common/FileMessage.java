package cn.edu.sustech.cs209.chatting.common;

import cn.edu.sustech.cs209.chatting.domain.User;

import java.io.File;

public class FileMessage extends ChatMessage {
    private Long timestamp;
    private User sentBy;
    private Integer chatId;
    private File file;
    private byte[] content;

    public FileMessage(Long timestamp, User sentBy, Integer chatId, File file, byte[] content) {
        super(timestamp, sentBy, chatId, "[FILE]:" + file.getName());
        this.timestamp = timestamp;
        this.sentBy = sentBy;
        this.chatId = chatId;
        this.content = content;
    }

    @Override
    public Long timestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public User sentBy() {
        return sentBy;
    }

    public void setSentBy(User sentBy) {
        this.sentBy = sentBy;
    }

    @Override
    public Integer chatId() {
        return chatId;
    }

    public void setChatId(Integer chatId) {
        this.chatId = chatId;
    }

    public File file() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public byte[] content() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
