package test;

import persistence.dto.UserDTO;
import util.OutputHandler;

import java.io.IOException;

public class UserService {

    public static UserDTO login(NetworkClient nc) throws IOException {
        return Util.loginProcess(nc);
    }

    public static void mainMenu(UserDTO user, NetworkClient nc) {
        while (true) {
            int choice = Util.showUserMainMenu();
            try {
                switch (choice) {
                    case 1 -> UserOrderService.orderMenu(user, nc);
                    case 2 -> UserOrderService.couponMenu(user, nc);
                    case 3 -> UsageService.showHistory(user, nc);
                    case 0 -> {
                        OutputHandler.showOut("로그아웃 되었습니다");
                        return;
                    }
                    default -> OutputHandler.showFail("잘못된 선택입니다");
                }
            } catch (IOException e) {
                OutputHandler.showFail("에러: " + e.getMessage());
            }
        }
    }
}
