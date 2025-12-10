package service;

import client.NetworkClient;
import network.Protocol;
import network.ProtocolType;
import network.ProtocolCode;
import persistence.dao.RestaurantDAO;
import persistence.dto.MenuPriceDTO;
import persistence.dto.PaymentDTO;
import persistence.dto.RestaurantDTO;
import util.InputHandler;
import util.OutputHandler;
import util.TimeSlotUtil;

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
    public static void order() {
        // 1. 전체 식당 목록 조회
        RestaurantDAO restaurantDAO = new RestaurantDAO();
        List<RestaurantDTO> restaurants = restaurantDAO.findAllRestaurants();
        if (restaurants == null || restaurants.isEmpty()) {
            OutputHandler.showError("식당 정보가 없습니다.");
            return;
        }

        // 2. 사용자에게 식당 선택하도록 안내
        OutputHandler.showTitle("식당 목록");
        for (int i = 0; i < restaurants.size(); i++) {
            RestaurantDTO r = restaurants.get(i);
            System.out.println((i + 1) + ". " + r.getName());
        }

        int choice = InputHandler.getInt("식당 번호 선택");
        if (choice < 1 || choice > restaurants.size()) {
            OutputHandler.showError("잘못된 선택입니다.");
            return;
        }

        RestaurantDTO selectedRestaurant = restaurants.get(choice - 1);

        // 3. 메뉴 목록 조회
        String mealTime = TimeSlotUtil.getCurrentOrNearestMealTime(selectedRestaurant);
        MenuPriceDTO dto = new MenuPriceDTO();
        dto.setRestaurantId(selectedRestaurant.getRestaurantId());
        dto.setRestaurantName(selectedRestaurant.getName());
        dto.setMealTime(mealTime);

        Protocol response = NetworkClient.sendRequest(
                ProtocolCode.MENU_LIST_REQUEST,
                dto
        );

        if (!isResponseOK(response) ||
            response.getCode() != ProtocolCode.MENU_LIST_RESPONSE) {
            OutputHandler.showError("메뉴 조회 실패");
            return;
        }

        List<MenuPriceDTO> menus = (List<MenuPriceDTO>) response.getData();
        if (menus == null || menus.isEmpty()) {
            OutputHandler.showMessage("메뉴가 없습니다.");
            return;
        }

        // 4. 메뉴 출력 및 주문/이미지 선택
        String role = UserSession.getRole();
        if (role == null) role = "student";

        OutputHandler.showTitle("메뉴 목록");
        for (int i = 0; i < menus.size(); i++) {
            MenuPriceDTO m = menus.get(i);
            int appliedPrice = "student".equals(role) ? m.getPriceStu() : m.getPriceFac();
            System.out.println((i + 1) + ". " + m.getMenuName() + " - " + appliedPrice + "원");
        }

        boolean loop = true;
        while (loop) {
            int action = InputHandler.getInt("1:주문 2:이미지 다운로드 3:뒤로");
            switch (action) {
                case 1 -> loop = orderByMenu(menus);
                case 2 -> imageDownload();
                case 3 -> loop = false;
                default -> OutputHandler.showError("잘못된 입력입니다.");
            }
        }
    }


    /* --------------------------------------------------------
     * 2. 메뉴 상세 조회 및 주문
     * -------------------------------------------------------- */
    private static boolean orderByMenu(List<MenuPriceDTO> menus) {
        int menuChoice = InputHandler.getInt("주문할 메뉴 번호");
        if (menuChoice < 1 || menuChoice > menus.size()) {
            OutputHandler.showError("잘못된 메뉴 번호");
            return true;
        }

        MenuPriceDTO menu = menus.get(menuChoice - 1);
        if (menu == null) {
            OutputHandler.showError("메뉴 정보가 비었습니다.");
            return true;
        }

        int userId = UserSession.getUserId();
        String role = UserSession.getRole();
        if (role == null) role = "student"; // 기본값

        int appliedPrice = "student".equals(role) ? menu.getPriceStu() : menu.getPriceFac();

        OutputHandler.showMessage("선택한 메뉴: " + menu.getMenuName());
        OutputHandler.showMessage("가격: " + appliedPrice + "원");

        OutputHandler.showTitle("주문 확인 (Y/N)");
        char ans = InputHandler.getChar("입력");
        if (ans == 'N' || ans == 'n') return true;

        // 공통 DTO 준비
        PaymentDTO baseDto = new PaymentDTO();
        baseDto.setUserId(userId);
        baseDto.setMenuPriceId(menu.getMenuPriceId());
        baseDto.setMenuName(menu.getMenuName());
        baseDto.setMenuPriceAtTime(appliedPrice);
        baseDto.setRestaurantId(menu.getRestaurantId());
        baseDto.setRestaurantName(menu.getRestaurantName());
        baseDto.setUserType(role);

        // 1. 쿠폰 결제 (선택)
        char couponAns = InputHandler.getChar("쿠폰 사용? (Y/N)");
        if (couponAns == 'Y' || couponAns == 'y') {
            int couponId = InputHandler.getInt("사용할 쿠폰 ID");

            PaymentDTO couponDto = new PaymentDTO();
            couponDto.setUserId(baseDto.getUserId());
            couponDto.setMenuPriceId(baseDto.getMenuPriceId());
            couponDto.setMenuName(baseDto.getMenuName());
            couponDto.setMenuPriceAtTime(baseDto.getMenuPriceAtTime());
            couponDto.setRestaurantId(baseDto.getRestaurantId());
            couponDto.setRestaurantName(baseDto.getRestaurantName());
            couponDto.setUserType(baseDto.getUserType());
            couponDto.setUsedCouponId(couponId);

            Protocol res = NetworkClient.sendRequest(
                    ProtocolCode.PAYMENT_COUPON_REQUEST,
                    couponDto
            );

            if (res != null && res.getCode() == ProtocolCode.SUCCESS) {
                OutputHandler.showSuccess("쿠폰 결제 완료");
            } else {
                OutputHandler.showError("쿠폰 결제 실패");
            }
        }

        // 2. 카드 결제 (남은 결제)
        char cardAns = InputHandler.getChar("카드로 추가 결제? (Y/N)");
        if (cardAns == 'Y' || cardAns == 'y') {
            PaymentDTO cardDto = new PaymentDTO();
            cardDto.setUserId(baseDto.getUserId());
            cardDto.setMenuPriceId(baseDto.getMenuPriceId());
            cardDto.setMenuName(baseDto.getMenuName());
            cardDto.setMenuPriceAtTime(baseDto.getMenuPriceAtTime());
            cardDto.setRestaurantId(baseDto.getRestaurantId());
            cardDto.setRestaurantName(baseDto.getRestaurantName());
            cardDto.setUserType(baseDto.getUserType());

            Protocol cardRes = NetworkClient.sendRequest(
                    ProtocolCode.PAYMENT_CARD_REQUEST,
                    cardDto
            );

            if (cardRes != null && cardRes.getCode() == ProtocolCode.SUCCESS) {
                OutputHandler.showSuccess("카드 결제 완료");
            } else {
                OutputHandler.showError("카드 결제 실패");
            }
        }

        return true;
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
            OutputHandler.showError("이미지 다운로드 실패");
            return;
        }

        byte[] img = (byte[]) res.getData();
        if (img == null) {
            OutputHandler.showError("이미지 데이터가 비었습니다.");
            return;
        }

        try (FileOutputStream fos = new FileOutputStream("menu_" + menuId + ".jpg")) {
            fos.write(img);
            OutputHandler.showSuccess("이미지 다운로드 완료");
        } catch (Exception e) {
            OutputHandler.showError("이미지 저장 실패: " + e.getMessage());
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
            OutputHandler.showError("내역 조회 실패");
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
