package service;

import util.InputHandler;
import util.OutputHandler;
import persistence.dto.MenuPriceDTO;
import persistence.dto.CouponPolicyDTO;
import persistence.dto.PaymentDTO;
import network.ClientSocketHolder;
import network.Protocol;
import network.ProtocolCode;
import network.ProtocolType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminService {
    public static void mainService() {
        int choice;
        boolean isRunning = true;

        while (isRunning) {
            printMenu();
            choice = InputHandler.getInt("입력");

            switch (choice) {
                case 1 -> registerMenu();
                case 2 -> updateMenu();
                case 3 -> System.out.println("");
                case 4 -> couponPolicyMenu();
                case 5 -> paymentHistoryMenu();
                case 6 -> System.out.println("");
                case 7 -> isRunning = false;
                default -> OutputHandler.showError("잘못된 선택입니다");
            }
        }
    }

    private static void couponPolicyMenu() {
        boolean running = true;
        while (running) {
            OutputHandler.showBar();
            OutputHandler.showTitle("쿠폰 관리");
            OutputHandler.showMenu(1, "정책 조회");
            OutputHandler.showMenu(2, "정책 등록");
            OutputHandler.showMenu(3, "뒤로가기");
            OutputHandler.showBar();

            switch (InputHandler.getInt("입력")) {
                case 1 -> viewCouponPolicies();
                case 2 -> registerCouponPolicy();
                case 3 -> running = false;
                default -> OutputHandler.showError("잘못된 선택입니다");
            }
        }
    }

    private static void viewCouponPolicies() {
        Protocol res = sendRequest(new Protocol(ProtocolType.REQUEST, ProtocolCode.COUPON_POLICY_LIST_REQUEST, null));
        if (res == null || res.getCode() != ProtocolCode.COUPON_POLICY_LIST_RESPONSE) {
            OutputHandler.showError("정책 조회 실패");
            return;
        }
        @SuppressWarnings("unchecked")
        List<CouponPolicyDTO> list = (List<CouponPolicyDTO>) res.getData();
        if (list == null || list.isEmpty()) {
            OutputHandler.showMessage("등록된 정책이 없습니다.");
            return;
        }
        OutputHandler.showTitle("쿠폰 정책 목록");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (CouponPolicyDTO p : list) {
            String eff = p.getEffectiveDate() == null ? "-" : p.getEffectiveDate().format(fmt);
            OutputHandler.showMessage("ID: " + p.getPolicyId() + " | 가격: " + p.getCouponPrice() + " | 적용: " + eff);
        }
    }

    private static void registerCouponPolicy() {
        int price = InputHandler.getInt("쿠폰 가격(원)");
        if (price <= 0) {
            OutputHandler.showError("가격은 양수여야 합니다.");
            return;
        }
        String dateStr = InputHandler.getString("적용 시작 시각 입력 (yyyy-MM-dd HH:mm, 비우면 지금)");

        CouponPolicyDTO dto = new CouponPolicyDTO();
        dto.setCouponPrice(price);
        if (dateStr != null && !dateStr.isBlank()) {
            try {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                dto.setEffectiveDate(LocalDateTime.parse(dateStr, fmt));
            } catch (DateTimeParseException e) {
                OutputHandler.showError("날짜 형식이 올바르지 않습니다.");
                return;
            }
        }

        Protocol res = sendRequest(new Protocol(ProtocolType.REQUEST, ProtocolCode.COUPON_POLICY_INSERT_REQUEST, dto));
        if (res != null && res.getCode() == ProtocolCode.SUCCESS) {
            OutputHandler.showSuccess("정책이 등록되었습니다.");
        } else {
            OutputHandler.showError("정책 등록 실패");
        }
    }

    private static void paymentHistoryMenu() {
        boolean running = true;
        while (running) {
            OutputHandler.showBar();
            OutputHandler.showTitle("주문/결제 내역 조회");
            OutputHandler.showMenu(1, "식당별 내역");
            OutputHandler.showMenu(2, "기간별 내역");
            OutputHandler.showMenu(3, "식당+기간 내역");
            OutputHandler.showMenu(4, "뒤로가기");
            OutputHandler.showBar();

            switch (InputHandler.getInt("입력")) {
                case 1 -> fetchPaymentHistory(selectRestaurantId(), null, null);
                case 2 -> {
                    LocalDateTime[] range = askPeriod();
                    if (range != null) fetchPaymentHistory(null, range[0], range[1]);
                }
                case 3 -> {
                    int rid = selectRestaurantId();
                    LocalDateTime[] range = askPeriod();
                    if (range != null) fetchPaymentHistory(rid, range[0], range[1]);
                }
                case 4 -> running = false;
                default -> OutputHandler.showError("잘못된 선택입니다");
            }
        }
    }

    private static LocalDateTime[] askPeriod() {
        String startStr = InputHandler.getString("시작 시각 (yyyy-MM-dd HH:mm)");
        String endStr = InputHandler.getString("종료 시각 (yyyy-MM-dd HH:mm)");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        try {
            LocalDateTime start = LocalDateTime.parse(startStr, fmt);
            LocalDateTime end = LocalDateTime.parse(endStr, fmt);
            return new LocalDateTime[]{start, end};
        } catch (DateTimeParseException e) {
            OutputHandler.showError("날짜 형식이 올바르지 않습니다.");
            return null;
        }
    }

    private static int selectRestaurantId() {
        OutputHandler.showMessage("식당 선택: 1) stdCafeteria  2) facCafeteria  3) snack");
        int restaurantChoice = InputHandler.getInt("선택(1~3)");
        int restaurantId = mapRestaurantId(restaurantChoice);
        if (restaurantId == -1) {
            OutputHandler.showError("잘못된 식당 선택입니다.");
        }
        return restaurantId;
    }

    private static void fetchPaymentHistory(Integer restaurantId, LocalDateTime start, LocalDateTime end) {
        Map<String, Object> payload = new HashMap<>();
        if (restaurantId != null && restaurantId > 0) {
            payload.put("restaurantId", restaurantId);
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        if (start != null && end != null) {
            payload.put("start", start.format(fmt));
            payload.put("end", end.format(fmt));
        }

        Protocol res = sendRequest(new Protocol(ProtocolType.REQUEST, ProtocolCode.ORDER_PAYMENT_HISTORY_REQUEST, payload));
        if (res == null) {
            OutputHandler.showError("조회 실패(응답 없음)");
            return;
        }
        if (res.getCode() != ProtocolCode.ORDER_PAYMENT_HISTORY_RESPONSE) {
            OutputHandler.showError("조회 실패: 코드 " + res.getCode());
            return;
        }

        @SuppressWarnings("unchecked")
        List<PaymentDTO> list = (List<PaymentDTO>) res.getData();
        if (list == null || list.isEmpty()) {
            OutputHandler.showMessage("내역이 없습니다.");
            return;
        }

        OutputHandler.showTitle("결제 내역");
        for (PaymentDTO p : list) {
            String time = p.getPaymentTime() == null ? "-" : p.getPaymentTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            int total = p.getCouponValueUsed() + p.getAdditionalCardAmount();
            OutputHandler.showMessage(
                    time + " | " +
                            p.getRestaurantName() + " | " +
                            p.getMenuName() + " | " +
                            total + "원 (쿠폰 " + p.getCouponValueUsed() + ", 카드 " + p.getAdditionalCardAmount() + ")"
            );
        }
    }

    private static void printMenu() {
        OutputHandler.showBar();
        OutputHandler.showTitle("관리자 메뉴");
        OutputHandler.showMenu(1, "메뉴 등록");
        OutputHandler.showMenu(2, "메뉴 수정(메뉴, 가격)");
        OutputHandler.showMenu(3, "메뉴 사진 등록");
        OutputHandler.showMenu(4, "쿠폰 관리");
        OutputHandler.showMenu(5, "현황 조회");
        OutputHandler.showMenu(6, "CSV 관리");
        OutputHandler.showMenu(7, "종료");
        OutputHandler.showBar();
    }

    /**
     * 메뉴 등록: 필수 필드 입력 후 DB에 저장.
     */
    private static void registerMenu() {
        OutputHandler.showTitle("메뉴 등록");
        OutputHandler.showMessage("식당 선택: 1) stdCafeteria  2) facCafeteria  3) snack");
        int restaurantChoice = InputHandler.getInt("선택(1~3)");
        int restaurantId = mapRestaurantId(restaurantChoice);
        String restaurantName = mapRestaurantName(restaurantChoice);
        if (restaurantId == -1 || restaurantName == null) {
            OutputHandler.showError("잘못된 식당 선택입니다.");
            return;
        }
        String semesterName = InputHandler.getString("학기명 (예: 2025-1)");
        char currentYN = InputHandler.getChar("현재 학기 적용? (Y/N)");
        boolean isCurrentSemester = (currentYN == 'Y');
        String mealTime = InputHandler.getString("식사 시간대 (아침/점심/저녁/상시)");
        String menuName = InputHandler.getString("메뉴 이름");
        String menuDate = InputHandler.getString("메뉴 날짜 (YYYY-MM-DD)");
        int priceStu = InputHandler.getInt("학생가");
        int priceFac = InputHandler.getInt("교직원가");

        MenuPriceDTO dto = new MenuPriceDTO();
        dto.setRestaurantId(restaurantId);
        dto.setRestaurantName(restaurantName);
        dto.setSemesterName(semesterName);
        dto.setCurrentSemester(isCurrentSemester);
        dto.setMealTime(mealTime);
        dto.setMenuName(menuName);
        dto.setMenuDate(menuDate);
        dto.setPriceStu(priceStu);
        dto.setPriceFac(priceFac);

        Protocol request = new Protocol(ProtocolType.REQUEST, ProtocolCode.MENU_INSERT_REQUEST, 0, dto);
        Protocol response = sendRequest(request);
        if (response != null && response.getCode() == ProtocolCode.SUCCESS) {
            OutputHandler.showSuccess("메뉴 등록 요청이 성공적으로 처리되었습니다.");
        } else {
            OutputHandler.showError("메뉴 등록 요청 실패");
            System.out.println(response.getCode());
        }
    }

    /**
     * 메뉴 수정: 식당 선택 -> 날짜 입력 -> 해당 날짜 식당의 메뉴 리스트 출력 -> menu_price_id 선택 -> 새 메뉴명 입력 -> 서버에 수정 요청
     */
    private static void updateMenu() {
        OutputHandler.showTitle("메뉴 수정");
        OutputHandler.showMessage("식당 선택: 1) stdCafeteria  2) facCafeteria  3) snack");
        int restaurantChoice = InputHandler.getInt("선택(1~3)");
        int restaurantId = mapRestaurantId(restaurantChoice);
        String restaurantName = mapRestaurantName(restaurantChoice);
        if (restaurantId == -1 || restaurantName == null) {
            OutputHandler.showError("잘못된 식당 선택입니다.");
            return;
        }

        String menuDate = InputHandler.getString("메뉴 날짜 (YYYY-MM-DD, 옵션)");
        String mealTime = InputHandler.getString("식사 시간대 (아침/점심/저녁/상시)");

        // 1) 메뉴 목록 요청
        Map<String, Object> listReq = new HashMap<>();
        listReq.put("restaurantId", restaurantId);
        listReq.put("menuDate", menuDate);

        Protocol listRequest = new Protocol(ProtocolType.REQUEST, ProtocolCode.MENU_LIST_REQUEST, listReq);
        Protocol listResponse = sendRequest(listRequest);
        if (listResponse == null || listResponse.getCode() != ProtocolCode.MENU_LIST_RESPONSE) {
            OutputHandler.showError("메뉴 목록을 불러오지 못했습니다.");
            return;
        }

        @SuppressWarnings("unchecked")
        List<MenuPriceDTO> menus = (List<MenuPriceDTO>) listResponse.getData();
        if (menus == null || menus.isEmpty()) {
            OutputHandler.showError("해당 조건의 메뉴가 없습니다.");
            return;
        }

        OutputHandler.showMessage("수정 대상 선택 (menu_price_id: menu_name):");
        for (MenuPriceDTO m : menus) {
            System.out.println(m.getMenuPriceId() + " : " + m.getMenuName());
        }

        int targetId = InputHandler.getInt("수정할 menu_price_id");
        String newMenuName = InputHandler.getString("새 메뉴 이름");
        String semesterName = InputHandler.getString("학기명 (예: 2025-1)");
        char currentYN = InputHandler.getChar("현재 학기 적용? (Y/N)");
        boolean isCurrentSemester = (currentYN == 'Y');
        int priceStu = InputHandler.getInt("학생가");
        int priceFac = InputHandler.getInt("교직원가");

        MenuPriceDTO dto = new MenuPriceDTO();
        dto.setMenuPriceId(targetId);
        dto.setRestaurantId(restaurantId);
        dto.setRestaurantName(restaurantName);
        dto.setSemesterName(semesterName);
        dto.setCurrentSemester(isCurrentSemester);
        dto.setMealTime(mealTime);
        dto.setMenuName(newMenuName);
        dto.setMenuDate(menuDate);
        dto.setPriceStu(priceStu);
        dto.setPriceFac(priceFac);

        Protocol updateRequest = new Protocol(ProtocolType.REQUEST, ProtocolCode.MENU_UPDATE_REQUEST, 0, dto);
        Protocol updateResponse = sendRequest(updateRequest);
        if (updateResponse != null && updateResponse.getCode() == ProtocolCode.SUCCESS) {
            OutputHandler.showSuccess("메뉴 수정 요청이 성공적으로 처리되었습니다.");
        } else {
            OutputHandler.showError("메뉴 수정 요청 실패");

        }
    }

    private static int mapRestaurantId(int choice) {
        return switch (choice) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            default -> -1;
        };
    }

    private static String mapRestaurantName(int choice) {
        return switch (choice) {
            case 1 -> "stdCafeteria";
            case 2 -> "facCafeteria";
            case 3 -> "snack";
            default -> null;
        };
    }

    // Client.java의 간단한 송수신 로직을 참고한 요청/응답 처리
    private static Protocol sendRequest(Protocol request) {
        try {
            ClientSocketHolder.os.write(request.getBytes());
            ClientSocketHolder.os.flush();

            byte[] header = new byte[6];
            int totalRead = 0;
            while (totalRead < 6) {
                int read = ClientSocketHolder.is.read(header, totalRead, 6 - totalRead);
                if (read == -1) break;
                totalRead += read;
            }
            if (totalRead < 6) {
                return null;
            }

            int dataLength = ((header[2] & 0xff) << 24) |
                    ((header[3] & 0xff) << 16) |
                    ((header[4] & 0xff) << 8) |
                    (header[5] & 0xff);

            byte[] body = new byte[dataLength];
            totalRead = 0;
            while (totalRead < dataLength) {
                int read = ClientSocketHolder.is.read(body, totalRead, dataLength - totalRead);
                if (read == -1) break;
                totalRead += read;
            }

            byte[] packet = new byte[6 + dataLength];
            System.arraycopy(header, 0, packet, 0, 6);
            System.arraycopy(body, 0, packet, 6, dataLength);
            return new Protocol(packet);
        } catch (Exception e) {
            OutputHandler.showError("요청 송수신 중 오류: " + e.getMessage());
            return null;
        }
    }
}
