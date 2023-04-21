package cn.edu.sustech.cs209.chatting.common;

import cn.edu.sustech.cs209.chatting.domain.User;

public record ChatMessage(Long timestamp, User sentBy, Integer chatId, String data) implements Information{

}
