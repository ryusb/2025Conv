package test;

import network.Protocol;
import network.ProtocolCode;
import network.ProtocolType;
import persistence.dto.PaymentDTO;
import persistence.dto.UserDTO;
import util.InputHandler;
import util.OutputHandler;

import java.io.IOException;
import java.util.List;

public class UsageService {
    public static void showHistory(UserDTO user, NetworkClient nc) throws IOException {
        if (user == null) {
            OutputHandler.showFail("사용자 정보가 없습니다");
            return;
        }

        while (true) {
            OutputHandler.showTitle("사용자 > 이용 내역");
            OutputHandler.showMenu(1, "결제 내역 조회");
            OutputHandler.showMenu(2, "쿠폰 내역 조회");
            OutputHandler.showMenu(0, "뒤로가기");
            int choice = InputHandler.getInt("");
            if (choice == 0) return;

            switch (choice) {
                case 1 -> {
                    // 결제 내역 조회
                    nc.send(new Protocol(ProtocolType.REQUEST, ProtocolCode.USAGE_HISTORY_REQUEST, user.getUserId()));
                    Protocol res = nc.receive();
                    if (res.getCode() == ProtocolCode.USAGE_HISTORY_RESPONSE) {
                        List<PaymentDTO> payments = (List<PaymentDTO>) res.getData();

                        OutputHandler.showDoubleBar();
                        OutputHandler.showMessage(" 📜 결제 내역 [" + payments.size() + "건]\n");

                        for (PaymentDTO p : payments) {
                            System.out.printf(" - %s  (%,d원)  %s\n", p.getMenuName(), p.getMenuPriceAtTime(), p.getPaymentTime());
                        }
                    } else {
                        Util.printFail(res);
                    }
                }
                case 2 -> {
                    // 쿠폰 내역 조회
                    nc.send(new Protocol(ProtocolType.REQUEST, ProtocolCode.COUPON_PURCHASE_HISTORY_REQUEST, user.getUserId()));
                    Protocol res = nc.receive();
                    if (res.getCode() == ProtocolCode.COUPON_PURCHASE_HISTORY_RESPONSE) {
                        List<?> coupons = (List<?>) res.getData();
                        OutputHandler.showDoubleBar();
                        OutputHandler.showMessage(" 🎟️ 쿠폰 내역 [" + coupons.size() + "건]\n");
                        for (Object cObj : coupons) {
                            persistence.dto.CouponDTO c = (persistence.dto.CouponDTO) cObj;
                            System.out.printf(
                                "  ID : %-4d | 가격 : %,7d | 구매 날짜 : %-10s\n",
                                c.getCouponId(),
                                c.getPurchaseValue(),
                                c.getPurchaseDate()
                            );
                        }
                    } else {
                        Util.printFail(res);
                    }
                }
                default -> OutputHandler.showFail("잘못된 선택입니다");
            }
        }
    }

}
