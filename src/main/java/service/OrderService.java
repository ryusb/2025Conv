package service;

import client.NetworkClient;
import network.Protocol;
import network.ProtocolType;
import network.ProtocolCode;
import persistence.dto.MenuPriceDTO;
import persistence.dto.PaymentDTO;
import util.InputHandler;
import util.OutputHandler;

import java.io.FileOutputStream;
import java.util.List;

public class OrderService {

    /* 공통 응답 검증 */
    private static boolean isResponseOK(Protocol res) {
        if (res == null) return false;
        return (res.getType() == ProtocolType.RESPONSE ||
                res.getType() == ProtocolType.RESULT);
    }


    /* --------------------------------------------------------
     * 1. 메뉴 목록 조회
     * -------------------------------------------------------- */
    public static void order(String restaurant) {

        Protocol response = NetworkClient.sendRequest(
                ProtocolCode.MENU_LIST_REQUEST,
                restaurant               // String → 목록 요청
        );

        if (!isResponseOK(response) ||
            response.getCode() != ProtocolCode.MENU_LIST_RESPONSE) {

            OutputHandler.showFail("메뉴 조회 실패");
            return;
        }

        List<MenuPriceDTO> menus = (List<MenuPriceDTO>) response.getData();
        if (menus == null || menus.isEmpty()) {
            OutputHandler.showMessage("메뉴가 없습니다.");
            return;
        }

        String role = UserSession.getRole();

        OutputHandler.showTitle("메뉴 목록");

        for (int i = 0; i < menus.size(); i++) {
            MenuPriceDTO m = menus.get(i);
            if (m == null) continue;

            int appliedPrice = role.equals("student")
                    ? m.getPriceStu()
                    : m.getPriceFac();

            System.out.println((i + 1) + ". " + m.getMenuName()
                    + " - " + appliedPrice + "원");
        }

        // 이후 메뉴 상세 or 이미지 선택
        boolean loop = true;
        while (loop) {
            int choice = InputHandler.getInt("입력");

            switch (choice) {
                case 1 -> loop = orderByRestaurant();
                case 2 -> imageDownload();
                case 3 -> loop = false;
                default -> OutputHandler.showFail("잘못된 입력입니다.");
            }
        }
    }


    /* --------------------------------------------------------
     * 2. 메뉴 상세 조회 (ProtocolCode 추가 없이 처리)
     * -------------------------------------------------------- */
    private static boolean orderByRestaurant() {
        int menuPriceId = InputHandler.getInt("주문할 메뉴 ID");
        int userId = UserSession.getUserId();
        String role = UserSession.getRole();

        // ⭐ 여기서도 동일한 프로토콜 사용
        Protocol menuRes = NetworkClient.sendRequest(
                ProtocolCode.MENU_LIST_REQUEST,
                menuPriceId              // int → 상세 메뉴 요청
        );

        if (!isResponseOK(menuRes) ||
            menuRes.getCode() != ProtocolCode.MENU_LIST_RESPONSE) {

            OutputHandler.showFail("메뉴 정보를 가져오지 못했습니다.");
            return false;
        }

        // 서버가 MenuPriceDTO 하나를 보내줘야 함
        MenuPriceDTO menu = (MenuPriceDTO) menuRes.getData();
        if (menu == null) {
            OutputHandler.showFail("메뉴 정보가 비었습니다.");
            return false;
        }

        int appliedPrice = role.equals("student") ? menu.getPriceStu() : menu.getPriceFac();

        OutputHandler.showMessage("선택한 메뉴: " + menu.getMenuName());
        OutputHandler.showMessage("가격: " + appliedPrice + "원");

        OutputHandler.showTitle("주문 확인 (Y/N)");
        char ans = InputHandler.getChar("입력");
        if (ans == 'N') return false;

        // 결제 정보 구성
        PaymentDTO dto = new PaymentDTO();
        dto.setUserId(userId);
        dto.setMenuPriceId(menuPriceId);
        dto.setMenuName(menu.getMenuName());
        dto.setMenuPriceAtTime(appliedPrice);
        dto.setRestaurantId(menu.getRestaurantId());
        dto.setRestaurantName(menu.getRestaurantName());
        dto.setUserType(role);

        // 쿠폰 결제
        boolean useCoupon = InputHandler.getChar("쿠폰 사용? (Y/N)") == 'Y';
        if (useCoupon) {
            Protocol res = NetworkClient.sendRequest(
                    ProtocolCode.PAYMENT_COUPON_REQUEST,
                    dto
            );
            if (res == null || res.getCode() != ProtocolCode.SUCCESS) {
                OutputHandler.showFail("쿠폰 결제 실패");
            }
        }

        // 카드 결제
        Protocol cardRes = NetworkClient.sendRequest(
                ProtocolCode.PAYMENT_CARD_REQUEST,
                dto
        );

        if (cardRes != null && cardRes.getCode() == ProtocolCode.SUCCESS) {
            OutputHandler.showSuccess("결제 완료");
        } else {
            OutputHandler.showFail("카드 결제 실패");
        }

        return false;
    }


    /* --------------------------------------------------------
     * 3. 이미지 다운로드
     * -------------------------------------------------------- */
    private static void imageDownload() {
        int menuId = InputHandler.getInt("이미지 받을 메뉴 ID");

        Protocol res = NetworkClient.sendRequest(
                ProtocolCode.MENU_IMAGE_DOWNLOAD_REQUEST,
                menuId
        );

        if (!isResponseOK(res) ||
            res.getCode() != ProtocolCode.MENU_IMAGE_RESPONSE) {

            OutputHandler.showFail("이미지 다운로드 실패");
            return;
        }

        byte[] img = (byte[]) res.getData();
        if (img == null) {
            OutputHandler.showFail("이미지 데이터가 비었습니다.");
            return;
        }

        try (FileOutputStream fos = new FileOutputStream("menu_" + menuId + ".jpg")) {
            fos.write(img);
            OutputHandler.showSuccess("이미지 다운로드 완료");
        } catch (Exception e) {
            OutputHandler.showFail("이미지 저장 실패");
        }
    }


    /* --------------------------------------------------------
     * 4. 결제 내역 조회
     * -------------------------------------------------------- */
    public static void paymentHistory() {
        int userId = UserSession.getUserId();

        Protocol response = NetworkClient.sendRequest(
                ProtocolCode.USAGE_HISTORY_REQUEST,
                userId
        );

        if (!isResponseOK(response) ||
            response.getCode() != ProtocolCode.USAGE_HISTORY_RESPONSE) {

            OutputHandler.showFail("내역 조회 실패");
            return;
        }

        List<PaymentDTO> list = (List<PaymentDTO>) response.getData();

        if (list == null || list.isEmpty()) {
            OutputHandler.showMessage("결제 내역이 없습니다.");
            return;
        }

        OutputHandler.showTitle("결제 내역");
        for (PaymentDTO p : list) {
            if (p == null) continue;
            System.out.println(p.getMenuName() + " - " + p.getMenuPriceAtTime() + "원");
        }
    }
}