package service;

import util.InputHandler;
import util.OutputHandler;

public class AdminService {
    public static void mainService() {
        int choice;
        boolean isRunning = true;

        while (isRunning) {
            printMenu();
            choice = InputHandler.getInt("입력");

            switch (choice) {
                case 1 -> System.out.println("");
                case 2 -> System.out.println("");
                case 3 -> System.out.println("");
                case 4 -> System.out.println("");
                case 5 -> System.out.println("");
                case 6 -> System.out.println("");
                case 7 -> isRunning = false;
                default -> OutputHandler.showError("잘못된 선택입니다");
            }
        }
    }

    private static void printMenu() {
        OutputHandler.showBar();
        OutputHandler.showTitle("관리자 메뉴");
        OutputHandler.showMenu(1, "메뉴 등록");
        OutputHandler.showMenu(2, "메뉴 수정(메뉴, 가격)");
        OutputHandler.showMenu(3, "메뉴 사진 등록");
        OutputHandler.showMenu(4, "쿠폰 관리");
        OutputHandler.showMenu(5, "현황 조회");
        OutputHandler.showMenu(6, "CSV 관리");
        OutputHandler.showMenu(7, "종료");
        OutputHandler.showBar();
    }
}
