package service;

import util.InputHandler;
import util.OutputHandler;

public class CouponService {
    // 잔여 쿠폰 조회
    public static void remainCoupon() {
        int couponQuantity = 2; // TODO : 쿠폰 조회
        int couponPrice = 20000;
        int userID = 2020;

        OutputHandler.showBar();
        OutputHandler.showTitle(userID + "님의 쿠폰");
        for (int i = 0; i < couponQuantity; i++) {
            OutputHandler.showMessage(couponPrice + "원 * "+ couponQuantity + "개");
        }
        OutputHandler.showBar();
    }

    // 쿠폰 구매
    public static void buyCoupon() {
        int couponQuantity = InputHandler.getInt("구매 수량 : ");
        if (couponQuantity > 0) {
            int couponPrice = 10000; // TODO : 결제 금액 가장 최근 정책에서 불러와지도록
            OutputHandler.showMessage("장당 쿠폰 금액 : " + couponPrice);
            OutputHandler.showMessage("결제 예정 금액 : " + couponQuantity * couponPrice);
            char ans = InputHandler.getChar("결제하시겠습니까? (Y/N) : ");

            if (ans == 'Y') {
                // TODO : 쿠폰 결제
            }
            else {
                OutputHandler.showPrompt("쿠폰 결제를 취소합니다");
            }
        }
        else {
            OutputHandler.showError("쿠폰 개수를 양수로 입력해 주세요");
        }
    }

    // 쿠폰 결제 내역 조회
    public static void paymentHistory() {
        // TODO : 쿠폰 결제 내역 조회
    }
}
