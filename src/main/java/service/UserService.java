package service;

import util.InputHandler;
import util.OutputHandler;

public class UserService {
    public static void mainService() {
        int choice;
        boolean isRunning = true;

        while (isRunning) {
            printMenu();
            choice = InputHandler.getInt("입력");

            switch (choice) {
                case 1 -> orderMenu();
                case 2 -> couponMenu();
                case 3 -> historyMenu();
                case 4 -> isRunning = false;
                default -> OutputHandler.showError("잘못된 선택입니다");
            }
        }
    }

    private static void printMenu() {
        OutputHandler.showBar();
        OutputHandler.showTitle("사용자 메뉴");
        OutputHandler.showMenu(1, "주문하기");
        OutputHandler.showMenu(2, "쿠폰 조회 및 구매");
        OutputHandler.showMenu(3, "결제 내역 조회");
        OutputHandler.showMenu(4, "종료");
        OutputHandler.showBar();
    }

    private static void orderMenu() {

    }


    private static void couponMenu() {

    }


    private static void historyMenu() {

    }


}
