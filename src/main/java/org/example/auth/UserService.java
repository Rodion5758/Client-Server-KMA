package org.example.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserService {
    private final Map<String, String> users = new ConcurrentHashMap<>();

    public UserService() {
        users.put("admin", "admin");
    }

    public boolean authenticate(String login, String password) {
        return password != null && password.equals(users.get(login));
    }
}
