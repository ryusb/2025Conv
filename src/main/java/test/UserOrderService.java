package test;

import persistence.dto.UserDTO;

import java.io.IOException;

public class UserOrderService {

    public static void orderMenu(UserDTO user, NetworkClient nc) throws IOException {
        while (true) {
            int choice = Util.showUserOrderMenu();
            if (choice == 0) return;

            switch (choice) {
                case 1 -> MenuService.viewMenuList(nc);
                case 2 -> MenuService.downloadMenuImage(nc);
                case 3 -> PaymentService.managePayment(user, nc);
                default -> System.out.println("잘못된 선택입니다.");
            }
        }
    }

    public static void couponMenu(UserDTO user, NetworkClient nc) throws IOException {
        CouponService.manageCoupon(user, nc);
    }

}
