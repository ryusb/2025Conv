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

            if (list == null || list.isEmpty()) {
                OutputHandler.showMessage("잔여 쿠폰이 없습니다.");
                return;
            }

            OutputHandler.showSingleBar();
            OutputHandler.showTitle(userId + "님의 쿠폰");

            for (CouponDTO c : list) {
                OutputHandler.showMessage(c.getPurchaseValue() + "원");
            }
            OutputHandler.showSingleBar();
        } else {
            OutputHandler.showFail("쿠폰 조회 실패");
        }
    }

    // 쿠폰 구매
    public static void buyCoupon() {
        int quantity = InputHandler.getInt("구매 수량");
        if (quantity <= 0) {
            OutputHandler.showFail("양수만 입력 가능");
            return;
        }

        // 정책 조회
        Protocol policyRes =
                NetworkClient.sendRequest(ProtocolCode.COUPON_POLICY_LIST_REQUEST, null);

        List<CouponPolicyDTO> policies = (List<CouponPolicyDTO>) policyRes.getData();

        // null 또는 빈 리스트 체크
        if (policies == null || policies.isEmpty()) {
            OutputHandler.showFail("쿠폰 정책이 없습니다.");
            return;
        }

        CouponPolicyDTO latest = policies.get(policies.size() - 1);
        int price = latest.getCouponPrice(); // CouponPolicyDTO 필드명

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
                OutputHandler.showFail("결제 실패");
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

            if (list == null || list.isEmpty()) {
                OutputHandler.showMessage("결제 내역이 없습니다.");
                return;
            }

            OutputHandler.showTitle("쿠폰 결제 내역");

            for (PaymentDTO p : list) {
                System.out.println(p.getMenuName() + " - " + p.getMenuPriceAtTime() + "원");
            }
        } else {
            OutputHandler.showFail("내역 조회 실패");
        }
    }
}
