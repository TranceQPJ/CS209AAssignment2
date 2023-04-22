package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.ChatMessage;
import cn.edu.sustech.cs209.chatting.common.Command;
import cn.edu.sustech.cs209.chatting.common.Communication;
import cn.edu.sustech.cs209.chatting.common.Information;
import cn.edu.sustech.cs209.chatting.domain.Chat;
import cn.edu.sustech.cs209.chatting.domain.Message;
import cn.edu.sustech.cs209.chatting.domain.User;
import cn.edu.sustech.cs209.chatting.utils.ChatDatabase;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.util.Pair;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

public class Main {
  private static final ObservableMap<User, ClientHandler> onlineUsers = FXCollections.observableHashMap();
  private static ChatDatabase dao;

  static {
    onlineUsers.addListener((MapChangeListener<User, ClientHandler>) change -> {
      User user = change.getKey();
      if (change.wasAdded()) {
        System.out.println(user + " joined!");
      } else if (change.wasRemoved()) {
        System.out.println(user + " exited!");
        try {
          change.getValueRemoved().socket.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
      broadcast(new Command.UpdateOnlineUserNumber(onlineUsers.size()), onlineUsers.values());
    });
  }

  public static void broadcast(Information information, Collection<ClientHandler> targets) {

    targets.forEach(clientHandler -> {
      try {
        clientHandler.sendInformation(information);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  public static void connect() throws SQLException {
    String url = "jdbc:postgresql://localhost:5432/chat";
    String user = "postgres";
    String password = "root";
    Connection connection = DriverManager.getConnection(url, user, password);
    dao = new ChatDatabase(connection);
  }

  public static void main(String[] args) throws IOException, SQLException {
    connect();
    Main server = new Main();
    server.start(Communication.PORT);
  }

  public void start(int portNumber) throws IOException {
    // create a server socket that listens on the specified port
    ServerSocket serverSocket = new ServerSocket(portNumber);

    System.out.println("Waiting for clients to connect...");

    // continuously listen for incoming client connections
    while (true) {
      // wait for a client to connect
      Socket clientSocket = serverSocket.accept();
      // create a new client handler to handle the client connection
      ClientHandler handler = new ClientHandler(clientSocket);
      // start a new thread to handle the client connection
      new Thread(handler).start();
    }
  }

  // client handler class to handle a single client connection
  private static class ClientHandler implements Runnable {
    public final Socket socket;
    private User user;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ClientHandler(Socket socket) {
      this.socket = socket;
    }

    @Override
    public void run() {
      try {
        // create input and output streams for the client socket
        in = new ObjectInputStream(socket.getInputStream());
        out = new ObjectOutputStream(socket.getOutputStream());

        // do something with the client socket here
        System.out.println("Client connected: " + socket.getInetAddress());

        // continuously read incoming messages from the client
        Information information;
        while (true) {
          information = (Information) in.readObject();
          System.out.println("get information:" + information);
          handleInformation(information);
        }
      } catch (ClassNotFoundException | SQLException e) {
        throw new RuntimeException(e);
      } catch (EOFException ignored) {
        // if we reach this point, the client closed the connection
        System.out.println("Client disconnected: " + socket.getInetAddress());
        onlineUsers.remove(user);
        // close the client socket when done
        try {
          socket.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }

      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    public void sendInformation(Information information) throws IOException {
      System.out.println("send message " + information + " to " + user);
      out.writeObject(information);
      out.flush();
    }

    private void handleChatMessage(ChatMessage chatMessage) throws SQLException, IOException {
      System.err.println("received information " + chatMessage + " is a chat message");
      Integer chatId = chatMessage.chatId();
      User sender = chatMessage.sentBy();
      List<User> users = dao.getUsersInChat(chatId);
      System.out.println(chatMessage + " sent at " + Communication.toDateTime(chatMessage.timestamp()));
      System.out.println(chatMessage + " is sending to " + users);
      for (User user : users) {
        if (onlineUsers.containsKey(user)) {
          onlineUsers.get(user).sendInformation(chatMessage);
        }
      }
      dao.addMessage(chatId, sender.userId(), chatMessage.data());
    }

    private void handleCommand(Command command) throws SQLException, IOException {
      System.err.println("received information " + command + " is a command");
      if (command instanceof Command.Login login) {
        System.err.println("information " + command + " try to login");
        User user;
        if ((user = dao.getUserByUsernameAndPassword(login.username(), login.password())) != null) {
          if (onlineUsers.containsKey(user)) {
            System.out.println("user " + user + "already online");
            sendInformation(new Command.AlreadyOnline());
          } else {
            onlineUsers.put(this.user = user, this);
            sendInformation(new Command.UserExists());
          }
        } else {
          sendInformation(new Command.UserNotFind());
        }
      } else if (command instanceof Command.Register register) {
        System.err.println("information " + command + " try to register");
        if (dao.getUserByUsername(register.username()) != null) {
          System.out.println(register + " register failed, username already exists");
          sendInformation(new Command.UserExists());
        } else {
          dao.createUser(register.username(), register.password());
          onlineUsers.put(this.user = dao.getUserByUsername(register.username()), this);
          System.out.println("register successfully");
          sendInformation(new Command.RegisterSuccess());
        }
      } else if (command instanceof Command.FindUser findUser) {
        System.err.println("information " + command + " try to find the user " + findUser.username());
        sendInformation(new Command.UserInformation(dao.getUserByUsername(findUser.username())));
      } else if (command instanceof Command.GetOnlineUserList) {
        Set<User> users = onlineUsers.keySet();
        System.out.println("online users: " + users);
        sendInformation(new Command.OnlineUserList(new ArrayList<>(users)));
      } else if (command instanceof Command.RequestAllChats requestAllChats) {
        Map<Integer, Pair<List<Message>, List<User>>> result = dao.getMessagesAndUsersByUsername(requestAllChats.username());
        Map<Integer, Pair<List<ChatMessage>, List<User>>> toSend = new HashMap<>();
        for (Map.Entry<Integer, Pair<List<Message>, List<User>>> entry : result.entrySet()) {
          toSend.put(entry.getKey(), new Pair<>(
                  entry.getValue()
                          .getKey()
                          .stream().
                          map(message -> {
                            try {
                              return new ChatMessage(message.sentTime().getTime(),
                                      dao.getUserById(message.senderId()),
                                      message.chatId(), message.messageContent()
                              );
                            } catch (SQLException e) {
                              throw new RuntimeException(e);
                            }
                          }).toList()
                  , entry.getValue().getValue()));
        }
        System.out.println("user " + requestAllChats.username() + " get his/her related messages " + toSend);
        sendInformation(new Command.ResponseAllChats(toSend));
      } else if (command instanceof Command.RequestNewChat requestNewChat) {
        System.out.println("a new chat was initialized: " + requestNewChat.users());
        dao.createChat();
        Chat chat = dao.getLatestChat();
        for (User u : requestNewChat.users()) {
          dao.addUserToChat(chat.chatId(), u.userId());
        }

        ArrayList<ClientHandler> targets = new ArrayList<>();
        for (User u : requestNewChat.users()) {
          if (onlineUsers.containsKey(u))
            targets.add(onlineUsers.get(u));
        }
        broadcast(new Command.ResponseNewChat(chat.chatId(), requestNewChat.users()), targets);
      } else {
        throw new IllegalArgumentException("command " + command + " has not been implemented!");
      }
    }

    public void handleInformation(Information information) throws IOException, SQLException {
      System.err.println("handling information " + information);
      if (information instanceof ChatMessage chatMessage) {
        handleChatMessage(chatMessage);
      } else if (information instanceof Command command) {
        handleCommand(command);
      }
    }
  }
}
