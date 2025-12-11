package test;

import network.Protocol;
import network.ProtocolCode;
import network.ProtocolType;
import persistence.dto.CouponPolicyDTO;
import persistence.dto.UserDTO;
import util.InputHandler;
import util.OutputHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CouponService {
    public static void manageCoupon(UserDTO user, NetworkClient nc) throws IOException {
        if (user == null) {
            OutputHandler.showFail("사용자 정보가 없습니다");
            return;
        }

        while (true) {
            int choice = Util.showUserCouponMenu();
            if (choice == 0) return;

            switch (choice) {
                case 1 -> viewCouponList(user, nc);
                case 2 -> purchaseCoupon(user, nc);
                case 3 -> viewPurchaseHistory(user, nc);
                default -> OutputHandler.showFail("잘못된 선택입니다");
            }
        }
    }

    private static void viewCouponList(UserDTO user, NetworkClient nc) throws IOException {
        nc.send(new Protocol(ProtocolType.REQUEST, ProtocolCode.COUPON_LIST_REQUEST, user.getUserId()));
        Protocol res = nc.receive();
        if (res.getCode() == ProtocolCode.COUPON_LIST_RESPONSE) {
            List<?> list = (List<?>) res.getData();
            OutputHandler.showDoubleBar();
            OutputHandler.showMessage(" 🎟️ 쿠폰 목록 [" + list.size() + "장]\n");

            for (Object cObj : list) {
                persistence.dto.CouponDTO c = (persistence.dto.CouponDTO) cObj;
                System.out.printf(
                    "  ID : %-4d | 가격 : %,6d | 구매 날짜 : %-10s\n",
                    c.getCouponId(),
                    c.getPurchaseValue(),
                    c.getPurchaseDate()
                );
            }
        } else Util.printFail(res);
    }

    private static void purchaseCoupon(UserDTO user, NetworkClient nc) throws IOException {
        int qty = InputHandler.getInt("쿠폰 구매 수량: ");
        Map<String, Integer> req = new HashMap<>();
        req.put("userId", user.getUserId());
        req.put("quantity", qty);

        nc.send(new Protocol(ProtocolType.REQUEST, ProtocolCode.COUPON_PURCHASE_REQUEST, req));
        Protocol res = nc.receive();
        Util.printSimpleResult(res);
    }

    private static void viewPurchaseHistory(UserDTO user, NetworkClient nc) throws IOException {
        nc.send(new Protocol(ProtocolType.REQUEST, ProtocolCode.COUPON_PURCHASE_HISTORY_REQUEST, user.getUserId()));
        Protocol res = nc.receive();
        if (res.getCode() == ProtocolCode.COUPON_PURCHASE_HISTORY_RESPONSE) {
            List<?> list = (List<?>) res.getData();
            OutputHandler.showDoubleBar();
            OutputHandler.showMessage(" 📜 쿠폰 구매 내역 [" + list.size() + "건]\n");

            for (Object cObj : list) {
                persistence.dto.CouponDTO c = (persistence.dto.CouponDTO) cObj;
                System.out.printf(
                    "  ID : %-4d | 가격 : %,6d | 구매 날짜 : %-10s | %s\n",
                    c.getCouponId(),
                    c.getPurchaseValue(),
                    c.getPurchaseDate(),
                    c.isUsed() ? "사용완료" : "미사용"
                );
            }
        } else Util.printFail(res);
    }


    // -------------------- 관리자용 쿠폰 정책 관리 --------------------
    public static void managePolicy(NetworkClient nc) throws IOException {
        while (true) {
            System.out.println("\n--- [관리자 > 쿠폰 정책] ---");
            System.out.println(" 1. 정책 목록 조회");
            System.out.println(" 2. 신규 정책 생성");
            System.out.println(" 0. 뒤로가기");
            System.out.print("선택>> ");

            int choice = Util.getIntInput();
            if (choice == 0) return;

            switch (choice) {
                case 1 -> viewPolicyList(nc);
                case 2 -> createPolicy(nc);
                default -> System.out.println("잘못된 선택");
            }
        }
    }

    private static void viewPolicyList(NetworkClient nc) throws IOException {
        nc.send(new Protocol(ProtocolType.REQUEST, ProtocolCode.COUPON_POLICY_LIST_REQUEST, null));
        Protocol res = nc.receive();

        if (res.getCode() == ProtocolCode.COUPON_POLICY_LIST_RESPONSE) {
            List<CouponPolicyDTO> list = (List<CouponPolicyDTO>) res.getData();
            System.out.println("📜 쿠폰 정책 목록:");
            for (CouponPolicyDTO p : list) {
                System.out.println("- 가격: " + p.getCouponPrice() + ", 적용일: " + p.getEffectiveDate());
            }
        } else {
            Util.printFail(res);
        }
    }

    private static void createPolicy(NetworkClient nc) throws IOException {
        System.out.println("\n[관리자: 쿠폰 정책 생성]");
        CouponPolicyDTO p = new CouponPolicyDTO();
        System.out.print("쿠폰 가격: ");
        p.setCouponPrice(Util.getIntInput());
        p.setEffectiveDate(LocalDateTime.now());

        nc.send(new Protocol(ProtocolType.REQUEST, ProtocolCode.COUPON_POLICY_INSERT_REQUEST, p));
        Util.printSimpleResult(nc.receive());
    }

}
