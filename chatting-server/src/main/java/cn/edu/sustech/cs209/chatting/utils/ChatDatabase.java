package cn.edu.sustech.cs209.chatting.utils;

import cn.edu.sustech.cs209.chatting.domain.Chat;
import cn.edu.sustech.cs209.chatting.domain.Message;
import cn.edu.sustech.cs209.chatting.domain.User;
import javafx.util.Pair;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatDatabase {
    private final Connection connection;

    public ChatDatabase(Connection connection) {
        this.connection = connection;
    }

    public List<User> getUsersInChat(int chatId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT \"user\".user_id, username, password " +
                        "FROM \"user\" " +
                        "JOIN chat_user ON \"user\".user_id = chat_user.user_id " +
                        "WHERE chat_user.chat_id = ?"
        );
        statement.setInt(1, chatId);
        ResultSet resultSet = statement.executeQuery();

        List<User> users = new ArrayList<>();
        while (resultSet.next()) {
            int userId = resultSet.getInt("user_id");
            String username = resultSet.getString("username");
            String password = resultSet.getString("password");
            users.add(new User(userId, username, password));
        }
        return users;
    }

    public User getUserById(int userId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT user_id, username, password FROM \"user\" WHERE user_id = ?");
        statement.setInt(1, userId);
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            String username = resultSet.getString("username");
            String password = resultSet.getString("password");
            return new User(userId, username, password);
        }
        return null;
    }

    public User getUserByUsername(String username) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT user_id, username, password FROM \"user\" WHERE username = ?");
        statement.setString(1, username);
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            int userId = resultSet.getInt("user_id");
            String password = resultSet.getString("password");
            return new User(userId, username, password);
        }
        return null;
    }

    public Map<Integer, Pair<List<Message>, List<User>>> getMessagesAndUsersByUsername(String username) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT chat_id " +
                        "FROM chat_user " +
                        "JOIN \"user\" ON chat_user.user_id = \"user\".user_id " +
                        "WHERE username = ?"
        );
        statement.setString(1, username);
        ResultSet resultSet = statement.executeQuery();

        Map<Integer, Pair<List<Message>, List<User>>> result = new HashMap<>();
        while (resultSet.next()) {
            int chatId = resultSet.getInt("chat_id");
            List<Message> messages = getMessagesByChatId(chatId);
            List<User> users = getUsersByChatId(chatId);
            result.put(chatId, new Pair<>(messages, users));
        }
        return result;
    }

    private List<Message> getMessagesByChatId(int chatId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT message_id, message_content, sender_id, sent_time " +
                        "FROM message " +
                        "WHERE chat_id = ?"
        );
        statement.setInt(1, chatId);
        ResultSet resultSet = statement.executeQuery();

        List<Message> messages = new ArrayList<>();
        while (resultSet.next()) {
            int messageId = resultSet.getInt("message_id");
            String messageContent = resultSet.getString("message_content");
            int senderId = resultSet.getInt("sender_id");
            Timestamp sentTime = resultSet.getTimestamp("sent_time");
            messages.add(new Message(messageId, messageContent,chatId,  senderId, sentTime));
        }
        return messages;
    }

    private List<User> getUsersByChatId(int chatId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT \"user\".user_id, username, password " +
                        "FROM chat_user " +
                        "JOIN \"user\" ON chat_user.user_id = \"user\".user_id " +
                        "WHERE chat_id = ?"
        );
        statement.setInt(1, chatId);
        ResultSet resultSet = statement.executeQuery();

        List<User> users = new ArrayList<>();
        while (resultSet.next()) {
            int userId = resultSet.getInt("user_id");
            String username = resultSet.getString("username");
            String password = resultSet.getString("password");
            users.add(new User(userId, username, password));
        }
        return users;
    }


    public void createUser(String username, String password) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("INSERT INTO \"user\" (username, password) VALUES (?, ?)");
        statement.setString(1, username);
        statement.setString(2, password);
        statement.executeUpdate();
    }

    public void createChat() throws SQLException {
        PreparedStatement statement = connection.prepareStatement("INSERT INTO chat DEFAULT VALUES");
        statement.executeUpdate();
    }

    public Chat getLatestChat() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT chat_id, chat_time FROM chat ORDER BY chat_time DESC LIMIT 1");
        if (resultSet.next()) {
            int chatId = resultSet.getInt("chat_id");
            Timestamp chatTime = resultSet.getTimestamp("chat_time");
            return new Chat(chatId, chatTime);
        }
        return null;
    }

    public void addUserToChat(int chatId, int userId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("INSERT INTO chat_user (chat_id, user_id) VALUES (?, ?)");
        statement.setInt(1, chatId);
        statement.setInt(2, userId);
        statement.executeUpdate();
    }

    public void addMessage(int chatId, int senderId, String messageContent) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("INSERT INTO message (chat_id, sender_id, message_content) VALUES (?, ?, ?)");
        statement.setInt(1, chatId);
        statement.setInt(2, senderId);
        statement.setString(3, messageContent);
        statement.executeUpdate();
    }

    public User getUserByUsernameAndPassword(String username, String password) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT user_id, username, password " +
                        "FROM \"user\" " +
                        "WHERE username = ? AND password = ?"
        );
        statement.setString(1, username);
        statement.setString(2, password);
        ResultSet resultSet = statement.executeQuery();

        if (resultSet.next()) {
            int userId = resultSet.getInt("user_id");
            return new User(userId, username, password);
        } else {
            return null;
        }
    }
}