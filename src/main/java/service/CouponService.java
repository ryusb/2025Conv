package service;

import client.NetworkClient;
import network.Protocol;
import network.ProtocolCode;
import persistence.dto.CouponDTO;
import persistence.dto.CouponPolicyDTO;
import persistence.dto.PaymentDTO;
import persistence.dto.UserDTO;
import util.InputHandler;
import util.OutputHandler;

import java.util.List;

public class CouponService {

    private static UserDTO currentUser;

    /* 잔여 쿠폰 조회 */
    public static void remainCoupon() {
        Protocol res = NetworkClient.sendRequest(ProtocolCode.COUPON_LIST_REQUEST, currentUser.getUserId());
        if (res.getCode() != ProtocolCode.COUPON_LIST_RESPONSE) {
            OutputHandler.showError("쿠폰 조회 실패 (Code=0x" + Integer.toHexString(res.getCode()) + ")");
            return;
        }

        List<CouponDTO> list = (List<CouponDTO>) res.getData();
        if (list == null || list.isEmpty()) {
            OutputHandler.showMessage("잔여 쿠폰이 없습니다.");
            return;
        }

        OutputHandler.showBar();
        OutputHandler.showTitle(currentUser.getLoginId() + "님의 쿠폰");
        for (CouponDTO c : list) {
            System.out.println("- " + c.getPurchaseValue() + "원 (" + c.getPurchaseDate() + ")");
        }
        OutputHandler.showBar();
    }

    /* 쿠폰 구매 */
    public static void buyCoupon() {
        int quantity = InputHandler.getInt("구매 수량");
        if (quantity <= 0) {
            OutputHandler.showError("1 이상 입력하세요.");
            return;
        }

        Protocol policyRes = NetworkClient.sendRequest(ProtocolCode.COUPON_POLICY_LIST_REQUEST, null);
        List<CouponPolicyDTO> policies = (List<CouponPolicyDTO>) policyRes.getData();
        if (policies == null || policies.isEmpty()) {
            OutputHandler.showError("쿠폰 정책이 없습니다.");
            return;
        }

        CouponPolicyDTO latest = policies.get(policies.size() - 1);
        int price = latest.getCouponPrice();
        OutputHandler.showMessage("장당 가격: " + price + "원 | 총 금액: " + (price * quantity) + "원");

        char ans = InputHandler.getChar("결제하시겠습니까? (Y/N)");
        if (ans != 'Y' && ans != 'y') {
            OutputHandler.showMessage("결제 취소");
            return;
        }

        // 요청 DTO 구성
        CouponDTO dto = new CouponDTO();
        dto.setUserId(currentUser.getUserId());

        Protocol res = NetworkClient.sendRequest(ProtocolCode.COUPON_PURCHASE_REQUEST, dto);
        switch (res.getCode()) {
            case ProtocolCode.SUCCESS -> OutputHandler.showSuccess("쿠폰 구매 성공");
            case ProtocolCode.FAIL -> OutputHandler.showError("구매 실패: " + res.getData());
            case ProtocolCode.PERMISSION_DENIED -> OutputHandler.showError("권한 없음");
            default -> OutputHandler.showError("알 수 없는 응답 코드: 0x" + Integer.toHexString(res.getCode()));
        }
    }

    /* 쿠폰 결제 내역 조회 */
    public static void paymentHistory() {
        Protocol res = NetworkClient.sendRequest(ProtocolCode.USAGE_HISTORY_REQUEST, currentUser.getUserId());
        if (res.getCode() != ProtocolCode.USAGE_HISTORY_RESPONSE) {
            OutputHandler.showError("결제 내역 조회 실패 (Code=0x" + Integer.toHexString(res.getCode()) + ")");
            return;
        }

        List<PaymentDTO> list = (List<PaymentDTO>) res.getData();
        if (list == null || list.isEmpty()) {
            OutputHandler.showMessage("결제 내역이 없습니다.");
            return;
        }

        OutputHandler.showTitle("쿠폰 결제 내역");
        for (PaymentDTO p : list) {
            System.out.printf("- %s | %d원 | %s\n", p.getMenuName(), p.getMenuPriceAtTime(), p.getPaymentTime());
        }
    }
}
