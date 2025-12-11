package test;

import network.Protocol;
import network.ProtocolCode;
import network.ProtocolType;
import persistence.dto.MenuPriceDTO;

import java.io.IOException;

public class PriceService {
    public static void managePrice(NetworkClient nc) throws IOException {
        while (true) {
            System.out.println("\n--- [관리자 > 가격 책정] ---");
            System.out.println(" 1. 분식당 개별 가격 등록");
            System.out.println(" 2. 일반식당 일괄 가격 등록");
            System.out.println(" 0. 뒤로가기");
            System.out.print("선택>> ");
            int choice = Util.getIntInput();
            if (choice == 0) return;
            switch (choice) {
                case 1 -> registerSnackPrice(nc);
                case 2 -> registerRegularPrice(nc);
                default -> System.out.println("잘못된 선택");
            }
        }
    }

    private static void registerSnackPrice(NetworkClient nc) throws IOException {
        registerPrice(nc, ProtocolCode.PRICE_REGISTER_SNACK_REQUEST);
    }

    private static void registerRegularPrice(NetworkClient nc) throws IOException {
        registerPrice(nc, ProtocolCode.PRICE_REGISTER_REGULAR_REQUEST);
    }

    private static void registerPrice(NetworkClient nc, byte code) throws IOException {
        System.out.println("\n[관리자: 가격 등록 (" + (code == ProtocolCode.PRICE_REGISTER_SNACK_REQUEST ? "분식" : "일괄") + ")]");
        MenuPriceDTO m = new MenuPriceDTO();
        java.util.Scanner sc = new java.util.Scanner(System.in);

        System.out.print("식당 ID: "); m.setRestaurantId(Util.getIntInput());
        System.out.print("학기명: "); m.setSemesterName(sc.nextLine());
        m.setCurrentSemester(true);
        System.out.print("학생가: "); m.setPriceStu(Util.getIntInput());
        System.out.print("교직원가: "); m.setPriceFac(Util.getIntInput());

        if (code == ProtocolCode.PRICE_REGISTER_SNACK_REQUEST) {
            System.out.print("메뉴명: "); m.setMenuName(sc.nextLine());
            System.out.print("식당명: "); m.setRestaurantName(sc.nextLine());
            System.out.print("시간대: "); m.setMealTime(sc.nextLine());
        }

        nc.send(new network.Protocol(network.ProtocolType.REQUEST, code, m));
        Util.printSimpleResult(nc.receive());
    }
}
