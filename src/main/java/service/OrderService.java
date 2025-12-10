package service;

import client.NetworkClient;
import network.Protocol;
import network.ProtocolCode;
import persistence.dto.MenuPriceDTO;
import persistence.dto.PaymentDTO;
import util.InputHandler;
import util.OutputHandler;

import java.util.List;
import java.io.FileOutputStream;

public class OrderService {
    // 음식 주문 -> 결제
    public static void order(String restaurant) {
        OutputHandler.showTitle("메뉴 조회");

        // 메뉴 요청
        Protocol response = NetworkClient.sendRequest(
                ProtocolCode.MENU_LIST_REQUEST,
                restaurant
        );

        if (response.getCode() == ProtocolCode.MENU_LIST_RESPONSE) {
            List<MenuPriceDTO> menus = (List<MenuPriceDTO>) response.getData();

            String role = UserSession.getRole();   // "student" 또는 "other"

            System.out.println();
            OutputHandler.showBar();
            OutputHandler.showTitle("메뉴 목록");

            for (int i = 0; i < menus.size(); i++) {
                MenuPriceDTO m = menus.get(i);

                int appliedPrice = role.equals("student")
                        ? m.getPriceStu()
                        : m.getPriceFac();

                System.out.println((i + 1) + ". " + m.getMenuName()
                        + " - " + appliedPrice + "원");
            }

            OutputHandler.showBar();
        }


        // 권한 체크 (예: 학생은 일부 메뉴 못 산다)
        if (!UserSession.hasPermission("ORDER")) {
            OutputHandler.showError("권한이 없습니다.");
            return;
        }

        // 이후 선택 루프는 기존 코드 그대로
        boolean isRetry = true;
        while (isRetry) {
            int choice = InputHandler.getInt("입력");
            OutputHandler.showTitle("상세 메뉴");
            OutputHandler.showTitle("메뉴 선택");

            switch (choice) {
                case 1 -> isRetry = orderByRestaurant();
                case 2 -> imageDownload();
                case 3 -> isRetry = false;
                default -> OutputHandler.showError("잘못된 입력입니다");
            }
        }
    }


    // 음식 결제 내역
    public static void paymentHistory() {
        int userId = UserSession.getUserId();

        Protocol response = NetworkClient.sendRequest(
                ProtocolCode.USAGE_HISTORY_REQUEST,
                userId
        );

        if (response.getCode() == ProtocolCode.USAGE_HISTORY_RESPONSE) {
            List<PaymentDTO> list = (List<PaymentDTO>) response.getData();
            OutputHandler.showTitle("결제 내역");

            for (PaymentDTO p : list) {
                System.out.println(p.getMenuName() + " - " + p.getMenuPriceAtTime() + "원");
            }
        } else {
            OutputHandler.showError("내역 조회 실패");
        }
    }


    private static void imageDownload() {
        int menuId = InputHandler.getInt("이미지 받을 메뉴 ID");

        Protocol response = NetworkClient.sendRequest(
                ProtocolCode.MENU_IMAGE_DOWNLOAD_REQUEST,
                menuId
        );

        if (response.getCode() == ProtocolCode.MENU_IMAGE_RESPONSE) {
            byte[] img = (byte[]) response.getData();

            try (FileOutputStream fos = new FileOutputStream("menu_" + menuId + ".jpg")) {
                fos.write(img);
                OutputHandler.showSuccess("이미지 다운로드 완료");
            } catch (Exception e) {
                OutputHandler.showError("이미지 저장 실패");
            }
        }
    }


    private static boolean orderByRestaurant() {
        int menuPriceId = InputHandler.getInt("주문할 메뉴 ID");  // menuPriceId 기준
        int userId = UserSession.getUserId();
        String role = UserSession.getRole();

        // ⭐ 메뉴 상세 정보 요청 (가격 포함)
        Protocol menuRes = NetworkClient.sendRequest(
                ProtocolCode.MENU_LIST_REQUEST,
                menuPriceId
        );

        if (menuRes.getCode() != ProtocolCode.MENU_LIST_RESPONSE) {
            OutputHandler.showError("메뉴 정보를 가져오지 못했습니다.");
            return false;
        }

        MenuPriceDTO menu = (MenuPriceDTO) menuRes.getData();

        // ⭐ 학생/직원별 가격 결정
        int appliedPrice = role.equals("student")
                ? menu.getPriceStu()
                : menu.getPriceFac();

        OutputHandler.showMessage("선택한 메뉴: " + menu.getMenuName());
        OutputHandler.showMessage("적용되는 가격: " + appliedPrice + "원");

        OutputHandler.showTitle("주문 확인 (Y/N)");
        char ans = InputHandler.getChar("입력");

        if (ans == 'N') return false;

        // ⭐ PaymentDTO 생성
        PaymentDTO dto = new PaymentDTO();
        dto.setUserId(userId);
        dto.setMenuPriceId(menuPriceId);
        dto.setMenuName(menu.getMenuName());
        dto.setMenuPriceAtTime(appliedPrice);   // ← 학생가/직원가가 최종 결제 가격
        dto.setRestaurantId(menu.getRestaurantId());
        dto.setRestaurantName(menu.getRestaurantName());
        dto.setUserType(role);

        OutputHandler.showTitle("결제하기");

        // 쿠폰 결제
        boolean useCoupon = InputHandler.getChar("쿠폰 사용? (Y/N)") == 'Y';
        if (useCoupon) {
            Protocol couponRes = NetworkClient.sendRequest(
                    ProtocolCode.PAYMENT_COUPON_REQUEST,
                    dto
            );
            if (couponRes.getCode() != ProtocolCode.SUCCESS) {
                OutputHandler.showError("쿠폰 결제 실패");
            }
        }

        // 카드 결제
        Protocol cardRes = NetworkClient.sendRequest(
                ProtocolCode.PAYMENT_CARD_REQUEST,
                dto
        );

        if (cardRes.getCode() == ProtocolCode.SUCCESS) {
            OutputHandler.showSuccess("결제 완료");
        } else {
            OutputHandler.showError("카드 결제 실패");
        }

        return false;
    }
}