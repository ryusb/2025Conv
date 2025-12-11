package test;

import persistence.dto.UserDTO;
import util.OutputHandler;

public class MainClient {
    private static UserDTO currentUser;
    private static final NetworkClient networkClient = new NetworkClient("118.216.49.188", 9000);

    public static void main(String[] args) {
        try {
            networkClient.connect();

            while (true) {
                if (currentUser == null) {
                    int choice = Util.showLoginMenu();
                    if (choice == 0) break;
                    if (choice == 1) currentUser = UserService.login(networkClient);
                } else {
                    String role = currentUser.getUserType();
                    if ("admin".equalsIgnoreCase(role)) {
                        AdminService.mainMenu(currentUser, networkClient);
                    } else {
                        UserService.mainMenu(currentUser, networkClient);
                        currentUser = null; // 로그아웃 처리
                    }
                }
            }

        } catch (Exception e) {
            OutputHandler.showFail("오류: " + e.getMessage());
        } finally {
            networkClient.close();
        }
    }
}
