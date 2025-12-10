package service;

import persistence.dto.UserDTO;

public class UserSession {

    private static UserDTO loginUser;

    public static void setUser(UserDTO user) {
        loginUser = user;
    }

    public static UserDTO getUser() {
        return loginUser;
    }

    public static int getUserId() {
        return loginUser != null ? loginUser.getUserId() : -1;
    }

    public static String getRole() {
        return loginUser != null ? loginUser.getUserType() : null;
    }

    public static boolean hasPermission(String key) {
        // 상황에 맞게 role 체크
        if (key.equals("ORDER")) {
            return !"admin".equals(getRole());
        }
        return true;
    }
}
