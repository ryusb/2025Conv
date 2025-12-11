package test;

import network.Protocol;
import network.ProtocolCode;
import network.ProtocolType;
import persistence.dto.PaymentDTO;
import persistence.dto.UserDTO;

import java.io.IOException;
import java.util.Scanner;

public class PaymentService {

    private static final Scanner sc = new Scanner(System.in);

    public static void managePayment(UserDTO user, NetworkClient nc) throws IOException {
        while (true) {
            int choice = Util.showUserPaymentMenu();
            if (choice == 0) return;

            switch (choice) {
                case 1 -> processPayment(user, nc, ProtocolCode.PAYMENT_CARD_REQUEST);
                case 2 -> processPayment(user, nc, ProtocolCode.PAYMENT_COUPON_REQUEST);
                default -> System.out.println("잘못된 선택입니다.");
            }
        }
    }

    private static void processPayment(UserDTO user, NetworkClient nc, byte code) throws IOException {
        PaymentDTO pay = new PaymentDTO();
        pay.setUserId(user.getUserId());
        pay.setUserType(user.getUserType());

        System.out.print("메뉴 ID: ");
        pay.setMenuPriceId(Util.getIntInput());

        if (code == ProtocolCode.PAYMENT_COUPON_REQUEST) {
            System.out.print("사용할 쿠폰 ID: ");
            pay.setUsedCouponId(Util.getIntInput());
        }

        nc.send(new Protocol(ProtocolType.REQUEST, code, pay));
        Protocol res = nc.receive();

        if (res.getCode() == ProtocolCode.SUCCESS && res.getData() instanceof PaymentDTO result) {
            System.out.println("✅ 결제 성공!");
            System.out.printf("   - 상태: %s\n   - 메뉴 가격: %d원\n   - 쿠폰 사용: %d원\n   - 추가 결제: %d원\n",
                    result.getStatus(), result.getMenuPriceAtTime(), result.getCouponValueUsed(), result.getAdditionalCardAmount());
        } else {
            Util.printFail(res);
        }
    }
}
