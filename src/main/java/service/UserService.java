package service;

import util.InputHandler;
import util.OutputHandler;

public class UserService {
    private static final String[] USER_MAIN_MENU = {
            "사용자 메뉴",
            "주문하기",
            "쿠폰 조회 및 구매",
            "결제 내역 조회",
            "종료"
    };
    private static final String[] USER_ORDER_MENU = {
            "식당 메뉴",
            "교직원 식당",
            "학생 식당",
            "분식당",
            "뒤로가기"
    };
    private static final String[] USER_COUPON_MENU = {
            "쿠폰 메뉴",
            "잔여 쿠폰",
            "쿠폰 구매",
            "뒤로가기"
    };
    private static final String[] USER_HISTORY_MENU = {
            "결제 내역 메뉴",
            "음식 결제 내역",
            "쿠폰 결제 내역",
            "뒤로가기"
    };

    public static void mainService() {
        int choice;
        boolean isRunning = true;

        while (isRunning) {
            printMenu(USER_MAIN_MENU);
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

    private static void printMenu(String[] menu) {
        System.out.println();
        OutputHandler.showBar();
        OutputHandler.showTitle(menu[0]);
        for (int i = 1; i < menu.length; i++) {
            OutputHandler.showMenu(i, menu[i]);
        }
        OutputHandler.showBar();
    }

    private static void orderMenu() {
        boolean isRunning = true;

        while (isRunning) {
            printMenu(USER_ORDER_MENU);

            switch (InputHandler.getInt("입력")) {
                case 1 -> OrderService.order("facCafeteria");
                case 2 -> OrderService.order("stdCafeteria");
                case 3 -> OrderService.order("snack");
                case 4 -> isRunning = false;
                default -> OutputHandler.showError("잘못된 선택입니다");
            }
        }
    }


    private static void couponMenu() {
        boolean isRunning = true;

        while (isRunning) {
            printMenu(USER_COUPON_MENU);

            switch (InputHandler.getInt("입력")) {
                case 1 -> CouponService.remainCoupon();
                case 2 -> CouponService.buyCoupon();
                case 3 -> isRunning = false;
                default -> OutputHandler.showError("잘못된 선택입니다");
            }
        }
    }


    private static void historyMenu() {
        boolean isRunning = true;

        while (isRunning) {
            printMenu(USER_HISTORY_MENU);

            switch (InputHandler.getInt("입력")) {
                case 1 -> OrderService.paymentHistory();
                case 2 -> CouponService.paymentHistory();
                case 3 -> isRunning = false;
                default -> OutputHandler.showError("잘못된 선택입니다");
            }
        }
    }
}
