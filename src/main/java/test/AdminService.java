package test;

import persistence.dto.UserDTO;
import util.OutputHandler;

import java.io.IOException;

public class AdminService {
    public static void mainMenu(UserDTO user, NetworkClient nc) {
        while (true) {
            int choice = Util.showAdminMainMenu();
            try {
                switch (choice) {
                    case 1 -> MenuService.manageMenu(nc);
                    case 2 -> PriceService.managePrice(nc);
                    case 3 -> CouponService.managePolicy(nc);
                    case 4 -> ReportService.viewReports(nc);
                    case 5 -> CsvService.manageData(nc);
                    case 0 -> { OutputHandler.showMessage("로그아웃"); return; }
                    default -> OutputHandler.showFail("잘못된 선택");
                }
            } catch (IOException e) {
                OutputHandler.showFail("에러: " + e.getMessage());
            }
        }
    }
}
