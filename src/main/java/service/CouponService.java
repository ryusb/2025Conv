package service;

import client.NetworkClient;
import network.Protocol;
import network.ProtocolCode;
import persistence.dto.CouponDTO;
import persistence.dto.CouponPolicyDTO;
import persistence.dto.PaymentDTO;
import util.InputHandler;
import util.OutputHandler;

import java.util.List;

public class CouponService {

    // 잔여 쿠폰 조회
    public static void remainCoupon() {
        int userId = UserSession.getUserId();

        Protocol response = NetworkClient.sendRequest(
                ProtocolCode.COUPON_LIST_REQUEST,
                userId
        );

        if (response.getCode() == ProtocolCode.COUPON_LIST_RESPONSE) {
            List<CouponDTO> list = (List<CouponDTO>) response.getData();

            OutputHandler.showBar();
            OutputHandler.showTitle(userId + "님의 쿠폰");

            for (CouponDTO c : list) {
                OutputHandler.showMessage(c.getPurchaseValue() + "원");
            }
            OutputHandler.showBar();
        }
    }

    // 쿠폰 구매
    public static void buyCoupon() {
        int quantity = InputHandler.getInt("구매 수량");
        if (quantity <= 0) {
            OutputHandler.showError("양수만 입력 가능");
            return;
        }

        // 정책 조회
        Protocol policyRes =
                NetworkClient.sendRequest(ProtocolCode.COUPON_POLICY_LIST_REQUEST, null);

        List<CouponPolicyDTO> policies =
                (List<CouponPolicyDTO>) policyRes.getData();

        CouponPolicyDTO latest = policies.get(policies.size() - 1);

        // 🔥 필드명 수정 (getPrice → getCouponPrice)
        int price = latest.getCouponPrice();

        OutputHandler.showMessage("장당 쿠폰 가격: " + price + "원");
        OutputHandler.showMessage("총 결제 금액: " + (price * quantity) + "원");

        char ans = InputHandler.getChar("결제하시겠습니까? (Y/N) : ");

        if (ans == 'Y') {
            CouponPolicyDTO dto = new CouponPolicyDTO();

            Protocol result = NetworkClient.sendRequest(
                    ProtocolCode.COUPON_PURCHASE_REQUEST,
                    dto
            );

            if (result.getCode() == ProtocolCode.SUCCESS) {
                OutputHandler.showSuccess("쿠폰 결제 성공");
            } else {
                OutputHandler.showError("결제 실패");
            }
        } else {
            OutputHandler.showMessage("결제 취소");
        }
    }


    // 쿠폰 결제 내역 조회
    public static void paymentHistory() {
        int userId = UserSession.getUserId();

        Protocol response = NetworkClient.sendRequest(
                ProtocolCode.ORDER_PAYMENT_HISTORY_REQUEST,
                userId
        );

        if (response.getCode() == ProtocolCode.ORDER_PAYMENT_HISTORY_RESPONSE) {
            List<PaymentDTO> list = (List<PaymentDTO>) response.getData();

            OutputHandler.showTitle("쿠폰 결제 내역");

            for (PaymentDTO p : list) {
                // 🔥 수정: 가격은 menuPriceAtTime 사용
                System.out.println(
                        p.getMenuName() + " - " + p.getMenuPriceAtTime() + "원"
                );
            }

        } else {
            OutputHandler.showError("내역 조회 실패");
        }
    }
}
