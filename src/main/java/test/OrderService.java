package test;

import persistence.dto.UserDTO;
import java.io.IOException;

public class OrderService {

    // 주문 메뉴 관리
    public static void manageOrder(UserDTO user, NetworkClient nc) throws IOException {
        while (true) {
            System.out.println("\n--- [사용자 > 주문] ---");
            System.out.println(" 1. 메뉴 목록 조회");
            System.out.println(" 2. 메뉴 이미지 다운로드");
            System.out.println(" 3. 결제 하기");
            System.out.println("0. 뒤로가기");
            System.out.print("선택>> ");

            int choice = Util.getIntInput();
            if (choice == 0) return;

            switch (choice) {
                case 1 -> MenuService.viewMenuList(nc);
                case 2 -> MenuService.downloadMenuImage(nc);
                case 3 -> PaymentService.managePayment(user, nc);
                default -> System.out.println("잘못된 선택");
            }
        }
    }
}
