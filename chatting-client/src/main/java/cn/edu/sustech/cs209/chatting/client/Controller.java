package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.*;
import cn.edu.sustech.cs209.chatting.domain.User;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Pair;
import lombok.SneakyThrows;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Controller implements Initializable {

  public final BlockingQueue<Information> queue = new ArrayBlockingQueue<>(1);
  private final List<FileMessage> files = new ArrayList<>();
  public Label currentUsername;
  public Label currentOnlineCnt;
  public TextArea inputArea;
  @FXML
  public ListView<Pair<Integer, Pair<List<User>, ListView<ChatMessage>>>> chatListView;
  public User currentUser;
  @FXML
  public Button F601;
  @FXML
  public Button F602;
  @FXML
  public Button F603;
  @FXML
  public Button F604;
  @FXML
  public Button F605;
  @FXML
  public Button F606;
  public MessageHandler handler;
  @FXML
  ListView<ChatMessage> chatContentListView;
  private Socket socket;
  private ObjectInputStream in;
  private ObjectOutputStream out;

  private static Pair<Integer, Pair<List<User>, ListView<ChatMessage>>> checkAndFindChatId(int chatId, ListView<Pair<Integer, Pair<List<User>, ListView<ChatMessage>>>> chatListView) {
    List<Pair<Integer, Pair<List<User>, ListView<ChatMessage>>>> tmp = chatListView.getItems()
            .stream()
            .filter(integerPairPair -> integerPairPair.getKey().equals(chatId))
            .toList();
    if (tmp.size() > 1)
      throw new RuntimeException("duplicate chat id appear " + tmp);
    if (tmp.size() == 0)
      throw new RuntimeException("chat " + chatId + " doesn't exist, please check!");
    return tmp.get(0);
  }

  @SneakyThrows
  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    initConnection();
    login();
    initUI();
  }

  private void initUI() throws IOException, InterruptedException {
    //customize show style
    chatContentListView.setCellFactory(new MessageCellFactory());
    chatListView.setCellFactory(new Callback<>() {
      @Override
      public ListCell<Pair<Integer, Pair<List<User>, ListView<ChatMessage>>>> call(ListView<Pair<Integer, Pair<List<User>, ListView<ChatMessage>>>> param) {
        return new ListCell<>() {
          @Override
          protected void updateItem(Pair<Integer, Pair<List<User>, ListView<ChatMessage>>> item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || Objects.isNull(item)) {
              setText(null);
              setGraphic(null);
              return;
            }
            setText(Communication.toSimpleChatName(item.getValue().getKey()));
          }
        };
      }
    });
    chatListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      int index = chatListView.getSelectionModel().getSelectedIndex();
      System.out.println("index " + index + " is selected");
      chatContentListView.getItems().clear();
      if (index >= 0) {
        chatContentListView.getItems().addAll(chatListView.getItems().get(index).getValue().getValue().getItems());
      }
      System.out.println(chatContentListView.getItems());
    });

    //add listener
    chatContentListView.setOnMouseClicked(event -> {
      int index = chatContentListView.getSelectionModel().getSelectedIndex();
      System.out.println("message of " + index + " is selected");
      if (index < 0)
        return;
      ChatMessage message = chatContentListView.getItems().get(index);
      if (message instanceof FileMessage file) {
        FileChooser fileChooser = new FileChooser();
        File f = fileChooser.showSaveDialog(null);
        try (FileOutputStream stream = new FileOutputStream(f)) {
          stream.write(file.content());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });
    chatListView.setOnMouseClicked(event -> {
      int index = chatListView.getSelectionModel().getSelectedIndex();
      System.out.println("index " + index + " is selected");
      chatContentListView.getItems().clear();
      if (index >= 0) {
        chatContentListView.getItems().addAll(chatListView.getItems().get(index).getValue().getValue().getItems());
      }
    });
    Map<Integer, Pair<List<ChatMessage>, List<User>>> chats = sendAndReceive(new Command.RequestAllChats(currentUser.username()), Command.ResponseAllChats.class).chats();
    System.out.println("init all chats!");
//        System.out.println("-----");
    Set<Map.Entry<Integer, Pair<List<ChatMessage>, List<User>>>> set = chats.entrySet()
            .stream()
            .sorted(Comparator.comparing(entry -> -entry.getValue()
                    .getKey()
                    .stream()
                    .map(ChatMessage::timestamp)
                    .max(Comparator.comparingLong(Long::longValue))
                    .orElse(-1L)))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    for (Map.Entry<Integer, Pair<List<ChatMessage>, List<User>>> entry : set) {
      addChatInitMessages(entry.getKey(), entry.getValue().getKey(), entry.getValue().getValue());
    }
//        System.out.println("----");
  }

  private boolean addChatInitMessages(Integer chatId, List<ChatMessage> messages, List<User> users) {
    chatListView.getItems().add(new Pair<>(chatId, new Pair<>(users, new ListView<>())));
    return addMessageTo(chatId, messages, chatListView);
  }

  private void login() throws IOException, InterruptedException {
    boolean flag = false;
    String username = null;
    String password = null;
    while (!flag) {
      Optional<LoginResult> result = showCustomDialog();
      if (result.isPresent()) {
        LoginResult r = result.get();
        username = r.username;
        password = r.password;
        if (username.isEmpty() || password.isEmpty()) {
          new Alert(Alert.AlertType.INFORMATION, "content can not be empty", ButtonType.OK).showAndWait();
        } else if (r.loginType == LoginType.LOGIN) {
          System.out.println("login " + r);
          Information information = sendAndReceive(new Command.Login(username, password));
          System.out.println("get information " + information);
          if (information instanceof Command.UserExists) {
            flag = true;
          } else if (information instanceof Command.UserNotFind) {
            new Alert(Alert.AlertType.INFORMATION, "user or password is wrong, please try again", ButtonType.OK).showAndWait();
          } else if (information instanceof Command.AlreadyOnline) {
            new Alert(Alert.AlertType.INFORMATION, "the user already logged in!", ButtonType.OK).showAndWait();
          } else
            throw new RuntimeException();
        } else {
          System.out.println("register " + r);
          Information information = sendAndReceive(new Command.Register(username, password));
          if (information instanceof Command.RegisterSuccess) {
            flag = true;
          } else if (information instanceof Command.UserExists) {
            new Alert(Alert.AlertType.INFORMATION, "This user already exists, please try another username", ButtonType.OK).showAndWait();
          } else
            throw new RuntimeException();
        }
      }
    }
    //确定user存在后,找到该user的所有信息
    Information information = sendAndReceive(new Command.FindUser(username));
    if (!(information instanceof Command.UserInformation i))
      throw new RuntimeException();

    handler.user = this.currentUser = i.user();
    currentUsername.setText(this.currentUser.username());
  }

  public void doSendFile() {
    FileChooser fileChooser = new FileChooser();

    File selectedFile = fileChooser.showOpenDialog(null);
    File init = new File("file:D:/ProgramProjects/JavaProgram/CS209A/assignment2/chatting-client/src/main/resources");
    System.out.println(init.exists());
    fileChooser.setInitialDirectory(init);
    // handle selected file
    if (selectedFile == null)
      return;
    System.out.println("Selected file: " + selectedFile.getAbsolutePath());
    try {
      byte[] fileBytes = Files.readAllBytes(selectedFile.toPath());
      send(new FileMessage(Communication.timestamp(), currentUser, getChatId(), selectedFile, fileBytes));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void appendEmoji(ActionEvent event) {
    Object source = event.getSource();
    if (source instanceof Button button) {
      byte[] byteArray = null;
      if (button == F601)
        byteArray = new byte[]{(byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x81};
      else if (button == F602)
        byteArray = new byte[]{(byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x82};
      else if (button == F603)
        byteArray = new byte[]{(byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x83};
      else if (button == F604)
        byteArray = new byte[]{(byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x84};
      else if (button == F605)
        byteArray = new byte[]{(byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x85};
      else if (button == F606)
        byteArray = new byte[]{(byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x86};

      if (byteArray != null)
        inputArea.appendText(new String(byteArray, StandardCharsets.UTF_8));
    }
  }

  private Optional<LoginResult> showCustomDialog() {
    // 创建对话框
    Dialog<LoginResult> dialog = new Dialog<>();
    dialog.setTitle("Login");
    dialog.setHeaderText("Enter you username and password:");
    // 设置对话框的内容
    Label usernameLabel = new Label("username:");
    Label passwordLabel = new Label("password");
    TextField usernameField = new TextField();
    TextField passwordField = new TextField();
    GridPane content = new GridPane();
    content.setAlignment(Pos.CENTER);
    content.setHgap(10);
    content.setVgap(10);
    content.add(usernameLabel, 0, 0);
    content.add(usernameField, 1, 0);
    content.add(passwordLabel, 0, 1);
    content.add(passwordField, 1, 1);
    dialog.getDialogPane().setContent(content);
    // 添加按钮
    ButtonType loginButtonType = new ButtonType("login", ButtonBar.ButtonData.OK_DONE);
    ButtonType registerButtonType = new ButtonType("register", ButtonBar.ButtonData.CANCEL_CLOSE);
    dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, registerButtonType);
    // 处理结果
    dialog.setResultConverter(dialogButton -> {
      if (dialogButton == loginButtonType) {
        return new LoginResult(LoginType.LOGIN, usernameField.getText(), passwordField.getText());
      } else if (dialogButton == registerButtonType) {
        return new LoginResult(LoginType.REGISTER, usernameField.getText(), passwordField.getText());
      }
      return null;
    });
    return dialog.showAndWait();
  }

  public void initConnection() throws IOException {
    connect(Communication.HOST, Communication.PORT);
    handler = new MessageHandler(in, out, queue, chatListView, currentOnlineCnt, files);
    new Thread(handler).start();
  }

  public void connect(String hostName, int portNumber) throws IOException {
    socket = new Socket(hostName, portNumber);
    out = new ObjectOutputStream(socket.getOutputStream());
    in = new ObjectInputStream(socket.getInputStream());
    System.out.println("Connected to server: " + socket.getInetAddress());
  }

  /**
   * important
   * aimed to send message to server.
   */
  public void send(Information message) throws IOException {
    out.writeObject(message);
    out.flush();
  }

  public <T extends Information> T sendAndReceive(Information message, Class<T> receiveType) throws IOException, InterruptedException {
    Information information = sendAndReceive(message);
    if (receiveType.isInstance(information))
      return (T) information;
    throw new IllegalArgumentException();
  }

  public Information sendAndReceive(Information message) throws IOException, InterruptedException {
    out.writeObject(message);
    out.flush();

    return queue.take();
  }

  public void disconnect() throws IOException {
    System.out.println("sockets disconnected");
    socket.close();
  }

  public List<User> findOtherUsers() throws IOException, InterruptedException {
    Information information = sendAndReceive(new Command.GetOnlineUserList());
    if (!(information instanceof Command.OnlineUserList list))
      throw new RuntimeException();
    List<User> users = list.list();
    users.removeIf(u -> u.username().equals(currentUser.username()));
    return users;
  }

  public boolean addMessageTo(int chatId, ChatMessage message, ListView<Pair<Integer, Pair<List<User>, ListView<ChatMessage>>>> chatListView) {
    Pair<Integer, Pair<List<User>, ListView<ChatMessage>>> pair = chatListView.getItems().get(indexOfChat(chatListView, chatId));
    if (pair == null) {
      System.out.println("chat " + chatId + " not found");
      return false;
    }
    return pair.getValue().getValue().getItems().add(message);
  }

  public boolean addMessageTo(int chatId, Collection<ChatMessage> messages, ListView<Pair<Integer, Pair<List<User>, ListView<ChatMessage>>>> chatListView) {
    Pair<Integer, Pair<List<User>, ListView<ChatMessage>>> pair = chatListView.getItems().get(indexOfChat(chatListView, chatId));
    if (pair == null) {
      System.out.println("chat " + chatId + " not found");
      return false;
    }
    System.out.println(messages + " was added to " + chatId);
    return pair.getValue().getValue().getItems().addAll(messages);
  }

  private int indexOfChat(ListView<Pair<Integer, Pair<List<User>, ListView<ChatMessage>>>> chatListView, int chatId) {
    for (int i = 0; i < chatListView.getItems().size(); i++) {
      if (chatListView.getItems().get(i).getKey() == chatId) {
        return i;
      }
    }
    return -1;
  }

  @FXML
  public void createPrivateChat() throws IOException, InterruptedException {
    AtomicReference<User> targetUser = new AtomicReference<>();

    Stage stage = new Stage();
    ComboBox<User> userSel = new ComboBox<>();

    userSel.getItems().addAll(findOtherUsers());

    Button okBtn = new Button("OK");
    okBtn.setOnAction(e -> {
      targetUser.set(userSel.getSelectionModel().getSelectedItem());
      stage.close();
    });

    HBox box = new HBox(10);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(20, 20, 20, 20));
    box.getChildren().addAll(userSel, okBtn);
    stage.setScene(new Scene(box));
    stage.showAndWait();

    User user = targetUser.get();
    if (user == null)
      return;

    String newChatName = Communication.toChatName(List.of(currentUser, user));
    List<Integer> chatId = chatListView.getItems()
            .stream()
            .filter(pair -> newChatName.equals(Communication.toChatName(pair.getValue().getKey())))
            .map(Pair::getKey).toList();

    if (chatId.size() > 1)
      throw new RuntimeException("duplicate chats appear " + chatId);
    else if (chatId.size() == 1) {
      //the chat already exists
      chatListView.getSelectionModel().select(indexOfChat(chatListView, chatId.get(0)));
    } else {
      send(new Command.RequestNewChat(List.of(currentUser, user)));
    }
  }

  /**
   * A new dialog should contain a multi-select users, showing all user's username.
   * You can select several users that will be joined in the group chat, including yourself.
   * <p>
   * The naming rule for group chats is similar to WeChat:
   * If there are > 3 users: display the first three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for example:
   * UserA, UserB, UserC... (10)
   * If there are <= 3 users: do not display the ellipsis, for example:
   * UserA, UserB (2)
   */
  @FXML
  public void createGroupChat() throws IOException, InterruptedException {
// 创建一个新的Stage，并设置其内容
    Stage stage = new Stage();
    VBox vbox = new VBox();
    Scene scene = new Scene(vbox);
    stage.setScene(scene);

    Button confirmButton = new Button("Confirm");
    VBox box = new VBox(10);
    box.setPadding(new Insets(10));
// 将ListView和确认按钮添加到VBox中
    vbox.getChildren().addAll(box, confirmButton);

    List<User> users = findOtherUsers();
    List<CheckBox> boxes = new ArrayList<>();
    for (User user : users) {
      CheckBox check = new CheckBox(user.username());
      boxes.add(check);
      box.getChildren().add(check);
    }

    confirmButton.setOnAction(event -> {
      int size = boxes.stream().filter(CheckBox::isSelected).toList().size();
      if (size < 2) {
        Alert alert = new Alert(Alert.AlertType.WARNING, "Please select at least 2 users. only " + size + " users were selected");
        alert.showAndWait();
      } else {
        stage.close();
      }
    });

    stage.showAndWait();
    List<Integer> indexes = new ArrayList<>();
    for (int i = 0; i < boxes.size(); i++) {
      if (boxes.get(i).isSelected())
        indexes.add(i);
    }
    if (indexes.size() < 2)
      return;
    List<User> list = new ArrayList<>();
    for (Integer index : indexes) {
      list.add(users.get(index));
    }
    list.add(currentUser);
    send(new Command.RequestNewChat(list));
  }

  /**
   * Sends the message to the <b>currently selected</b> chat.
   * <p>
   * Blank messages are not allowed.
   * After sending the message, you should clear the text input field.
   */
  @FXML
  public void doSendMessage() {
    String content = inputArea.getText();
    if (content.isEmpty()) {
      new Alert(Alert.AlertType.INFORMATION, "can't send empty message!", ButtonType.OK).showAndWait();
      return;
    }
    int chatId = getChatId();
    try {
      send(new ChatMessage(Communication.timestamp(), currentUser, chatId, content));
    } catch (IOException ignored) {
    }
    inputArea.setText("");
  }

  private int getChatId() {
    if (chatListView.getSelectionModel().getSelectedIndex() == -1) {
      new Alert(Alert.AlertType.INFORMATION, "chat channel not selected!", ButtonType.OK).showAndWait();
      return -1;
    }
    return chatListView.getItems().get(chatListView.getSelectionModel().getSelectedIndex()).getKey();
  }

  private enum LoginType {
    LOGIN, REGISTER
  }

  private record LoginResult(LoginType loginType, String username, String password) {
  }

  /**
   * You may change the cell factory if you changed the design of {@code Message} model.
   * Hint: you may also define a cell factory for the chats displayed in the left panel, or simply override the toString method.
   */
  private class MessageCellFactory implements Callback<ListView<ChatMessage>, ListCell<ChatMessage>> {
    @Override
    public ListCell<ChatMessage> call(ListView<ChatMessage> param) {
      return new ListCell<>() {
        @Override
        public void updateItem(ChatMessage msg, boolean empty) {
          super.updateItem(msg, empty);
          if (empty || Objects.isNull(msg)) {
            setText(null);
            setGraphic(null);
            return;
          }

          HBox wrapper = new HBox();
          Label nameLabel = new Label("@" + Communication.toDateTime(msg.timestamp()) + "[user]: " + msg.sentBy().username());
          Label msgLabel = new Label(msg.data());

//                    nameLabel.setPrefSize(100, 20);
          nameLabel.setPrefHeight(20);
          nameLabel.setWrapText(true);
          nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

          if (currentUser.username().equals(msg.sentBy().username())) {
            wrapper.setAlignment(Pos.TOP_RIGHT);
            wrapper.getChildren().addAll(msgLabel, nameLabel);
            msgLabel.setPadding(new Insets(0, 20, 0, 0));
          } else {
            wrapper.setAlignment(Pos.TOP_LEFT);
            wrapper.getChildren().addAll(nameLabel, msgLabel);
            msgLabel.setPadding(new Insets(0, 0, 0, 20));
          }

          setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
          setGraphic(wrapper);
        }
      };
    }
  }

  private class MessageHandler implements Runnable {
    public final BlockingQueue<Information> queue;
    public final ListView<Pair<Integer, Pair<List<User>, ListView<ChatMessage>>>> chatListView;
    public final Label currentOnlineCnt;
    public final List<FileMessage> files;
    public User user;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public MessageHandler(ObjectInputStream in, ObjectOutputStream out,
                          BlockingQueue<Information> queue,
                          ListView<Pair<Integer, Pair<List<User>, ListView<ChatMessage>>>> chatListView,
                          Label currentOnlineCnt, List<FileMessage> files) {
      this.in = in;
      this.out = out;
      this.queue = queue;
      this.chatListView = chatListView;
      this.currentOnlineCnt = currentOnlineCnt;
      this.files = files;
    }

    @Override
    public void run() {
      while (true) {
        try {
          // Read a message from the server
          Object object = in.readObject();
          if (object instanceof ChatMessage chatMessage) {
            System.out.println("Received message " + chatMessage);
            if (chatMessage instanceof FileMessage file)
              files.add(file);
            Platform.runLater(
                    () -> {
                      addMessageTo(chatMessage.chatId(), chatMessage, chatListView);
                      Pair<Integer, Pair<List<User>, ListView<ChatMessage>>> chat = chatListView.getItems().get(indexOfChat(chatListView, chatMessage.chatId()));
                      chatListView.getItems().remove(chat);
                      chatListView.getItems().add(0, chat);
                      chatListView.getSelectionModel().select(-1);
                      chatListView.getSelectionModel().select(0);
                    }
            );
          } else if (object instanceof Command.UpdateOnlineUserNumber update) {
            System.out.println(user + " updates online user number to " + update.userNumber());
            Platform.runLater(() -> currentOnlineCnt.setText("online users: " + update.userNumber()));
          } else if (object instanceof Command command) {
            if (command instanceof Command.UserExists
                    || command instanceof Command.LoginSuccess
                    || command instanceof Command.RegisterSuccess
                    || command instanceof Command.UserNotFind
                    || command instanceof Command.UserInformation
                    || command instanceof Command.OnlineUserList
                    || command instanceof Command.ResponseAllChats
                    || command instanceof Command.AlreadyOnline) {
              queue.put(command);
            } else if (command instanceof Command.ResponseNewChat newChat) {
              System.out.println("client creates a chat: " + newChat);
              Platform.runLater(() -> chatListView.getItems().add(new Pair<>(
                      newChat.chatId(), new Pair<>(
                      newChat.users(),
                      new ListView<>()
              ))));
            } else {
              throw new RuntimeException("unknown command " + command);
            }
          }

        } catch (SocketException e) {
//                    e.printStackTrace();
          Platform.runLater(() ->
                  {
                    try {
                      new Alert(Alert.AlertType.INFORMATION, "server closed!", ButtonType.OK)
                              .showAndWait();
                    } catch (Exception ignored) {
                    }
                  }
          );
//                    System.out.println("socket closed!");
          break;
        } catch (IOException | ClassNotFoundException e) {
          e.printStackTrace();
          break;
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }
}
