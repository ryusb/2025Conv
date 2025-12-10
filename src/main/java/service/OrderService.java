package service;

import util.InputHandler;
import util.OutputHandler;

public class OrderService {
    // 음식 주문 -> 결제
    public static void order(String restaurant) {
        // TODO : 식당 메뉴 조회
        OutputHandler.showTitle("메뉴 조회");

        // TODO : 권한체크
        boolean isRetry = true;
        while (isRetry) {
            int choice = InputHandler.getInt("입력");
            OutputHandler.showTitle("상세 메뉴"); // 메뉴 주문하기, 이미지 다운로드
            OutputHandler.showTitle("메뉴 선택");

            switch (choice) {
                case 1 -> isRetry = orderByRestaurant();
                case 2 -> imageDownload();
                case 3 -> isRetry = false;
                default -> OutputHandler.showError("잘못된 입력입니다");
            }
        }
    }

    // 음식 결제 내역
    public static void paymentHistory() {
        // TODO : 음식 결제 내역 조회
    }

    private static void imageDownload() {
        // TODO : 이미지 다운로드
    }

    private static boolean orderByRestaurant() {
        // TODO : 식당별 주문
        OutputHandler.showTitle("주문 확인 (Y/N) ");
        OutputHandler.showTitle("결제하기");

        // 카드 또는 쿠폰
        if (true) { // 쿠폰 사용 여부
            // TODO : 쿠폰 결제 로직
        }
        if (true) { // 남은 결제 금액 > 0
            // TODO : 카드 결제 로직
        }
        OutputHandler.showTitle("결제 완료");
        return false;
    }
}