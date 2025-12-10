package service;

import util.OutputHandler;

public class MainService {
    public static void run() {
        String role = login();
        switch (role) {
            case "admin" -> AdminService.mainService();
            case "other", "student" -> UserService.mainService();
            default -> OutputHandler.showError("Invalid role");
        }
    }

    private static String login() {
        return "admin";
    }
}
