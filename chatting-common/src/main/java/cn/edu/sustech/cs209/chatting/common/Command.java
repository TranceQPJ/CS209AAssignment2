package cn.edu.sustech.cs209.chatting.common;

import cn.edu.sustech.cs209.chatting.domain.User;
import javafx.util.Pair;

import java.util.List;
import java.util.Map;

/**
 * used to represent message between one client and the server
 */
public interface Command extends Information {
  record Register(String username, String password) implements Command {
  }

  record AlreadyOnline() implements Command {
  }

  record RegisterSuccess() implements Command {
  }

  record Login(String username, String password) implements Command {
  }

  record LoginSuccess(String username, String password) implements Command {
  }

  record IsUserExists(String username) implements Command {
  }

  record UserNotFind() implements Command {
  }

  record FindUser(String username) implements Command {
  }

  record UserInformation(User user) implements Command {
  }

  record UserExists() implements Command {
  }

  record AddMessage(ChatMessage chatMessage) implements Command {
  }

  record GetOnlineUserList() implements Command {
  }

  record GetRegisteredUserList(List<User> list) implements Command {
  }

  record OnlineUserList(List<User> list) implements Command {
  }

  record RequestNewChat(List<User> users) implements Command {
  }

  record ResponseAllChats(Map<Integer, Pair<List<ChatMessage>, List<User>>> chats) implements Command {
  }

  record RequestAllChats(String username) implements Command {
  }

  record ResponseNewChat(Integer chatId, List<User> users) implements Command {
  }

  record UpdateOnlineUserNumber(Integer userNumber) implements Command {
  }
}
