package cn.edu.sustech.cs209.chatting.common;

import cn.edu.sustech.cs209.chatting.domain.User;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

public final class Communication {
  public final static int PORT = 60000;
  public final static String HOST = "localhost";
  private final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  public static long timestamp() {
    return System.currentTimeMillis();
  }

  public static String toDateTime(long timestamp) {
    Date date = new Date(timestamp);
    return dateFormat.format(date);
  }

  public static String toSimpleChatName(Collection<User> collection) {
//        If there are > 3 users: display the first three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for example:
//     * UserA, UserB, UserC... (10)
//     * If there are <= 3 users: do not display the ellipsis, for example:
//     * UserA, UserB (2)
        /*String name = collection.stream()
                .map(User::username)
                .sorted()
                .limit(3)
                .reduce((s, s2) -> s + ", " + s2).get();
        if (collection.size() > 3)
            name += "...";
        return name + "(" + collection.size() + ")";*/
    return toChatName(collection);
  }

  public static String toChatName(Collection<User> collection) {
    return collection.stream()
            .map(User::username)
            .sorted()
            .reduce((s, s2) -> s + ", " + s2).get();
  }
}
