package client;

import network.Protocol;
import network.ProtocolCode;
import network.ProtocolType;
import persistence.dto.*;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class TestClient {
    // 서버 접속 정보
    private static final String SERVER_IP = "118.216.49.188"; // 또는 "localhost"
    private static final int PORT = 9000;

    private static Socket socket;
    private static InputStream is;
    private static OutputStream os;
    private static Scanner sc = new Scanner(System.in);

    // 현재 로그인한 유저 정보 (테스트용)
    private static UserDTO currentUser;

    public static void main(String[] args) {
        try {
            socket = new Socket(SERVER_IP, PORT);
            is = socket.getInputStream();
            os = socket.getOutputStream();

            System.out.println("🎉 [TestClient] 서버 연결 성공!");

            // 1. 로그인 루프 (성공할 때까지 or 종료)
            while (true) {
                if (currentUser == null) {
                    System.out.println("\n=== [시스템 접속] ===");
                    System.out.println("1. 로그인  0. 종료");
                    System.out.print("선택>> ");
                    int choice = getIntInput();

                    if (choice == 0) {
                        System.out.println("프로그램을 종료합니다.");
                        break;
                    }
                    if (choice == 1) {
                        login();
                    }
                } else {
                    // 2. 권한별 메뉴 분기
                    String role = currentUser.getUserType(); // "admin" or "student"/"facility"

                    // DB에 "admin"으로 저장되어 있는지 "관리자"로 저장되어 있는지에 따라 조건 수정 필요
                    // 여기서는 'admin' 문자열을 포함하거나 '관리자'인 경우 관리자로 취급
                    if ("admin".equalsIgnoreCase(role)) {
                        handleAdminMenu();
                    } else {
                        handleUserMenu();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("❌ 서버 연결 실패: " + e.getMessage());
        } finally {
            close();
        }
    }

    // ===============================================================
    // [메뉴 핸들링 로직]
    // ===============================================================

    // ===============================================================
    // [사용자 전용 메뉴 핸들링]
    // ===============================================================

    private static void handleUserMenu() {
        while (currentUser != null) {
            System.out.println("\n================ [사용자 메인] ================");
            System.out.println(" 1. 주문 하기 (메뉴/결제)");
            System.out.println(" 2. 쿠폰 관리");
            System.out.println(" 3. 이용 내역 조회");
            System.out.println(" 0. 로그아웃");
            System.out.print("선택>> ");

            int choice = getIntInput();
            try {
                switch (choice) {
                    case 1: handleUserOrderMenu(); break;
                    case 2: handleUserCouponMenu(); break;
                    case 3: testUsageHistory(); break;
                    case 0: currentUser = null; return;
                    default: System.out.println("잘못된 선택입니다.");
                }
            } catch (Exception e) {
                System.out.println("⚠️ 에러: " + e.getMessage());
            }
        }
    }

    // [1. 주문] 하위 메뉴
    private static void handleUserOrderMenu() throws IOException {
        while (true) {
            System.out.println("\n--- [사용자 > 주문] ---");
            System.out.println(" 1. 메뉴 목록 조회");
            System.out.println(" 2. 메뉴 이미지 다운로드");
            System.out.println(" 3. 결제 하기");
            System.out.println(" 0. 뒤로가기");
            System.out.print("선택>> ");

            int choice = getIntInput();
            if (choice == 0) return;

            switch (choice) {
                case 1: testMenuList(); break;
                case 2: testMenuImageDownload(); break;
                case 3: handleUserPaymentMenu(); break; // 결제 서브 메뉴로 이동
                default: System.out.println("잘못된 선택");
            }
        }
    }

    // [1-3. 결제] 하위 메뉴
    private static void handleUserPaymentMenu() throws IOException {
        while (true) {
            System.out.println("\n--- [사용자 > 주문 > 결제] ---");
            System.out.println(" 1. 카드 결제");
            System.out.println(" 2. 쿠폰 결제 (추가금 발생 가능)");
            System.out.println(" 0. 뒤로가기");
            System.out.print("선택>> ");

            int choice = getIntInput();
            if (choice == 0) return;

            switch (choice) {
                case 1: testPayment(ProtocolCode.PAYMENT_CARD_REQUEST); break;
                case 2: testPayment(ProtocolCode.PAYMENT_COUPON_REQUEST); break;
                default: System.out.println("잘못된 선택");
            }
        }
    }

    // [2. 쿠폰] 하위 메뉴
    private static void handleUserCouponMenu() throws IOException {
        while (true) {
            System.out.println("\n--- [사용자 > 쿠폰] ---");
            System.out.println(" 1. 내 쿠폰 조회");
            System.out.println(" 2. 쿠폰 구매");
            System.out.println(" 3. 쿠폰 구매 내역");
            System.out.println(" 0. 뒤로가기");
            System.out.print("선택>> ");

            int choice = getIntInput();
            if (choice == 0) return;

            switch (choice) {
                case 1: testCouponList(); break;
                case 2: testCouponPurchase(); break;
                case 3: testCouponPurchaseHistory(); break; // 쿠폰 사용 이력도 결제 내역에 포함됨
                default: System.out.println("잘못된 선택");
            }
        }
    }

    // ===============================================================
    // [관리자 전용 메뉴 핸들링]
    // ===============================================================
    private static void handleAdminMenu() {
        while (currentUser != null) {
            System.out.println("\n================ [관리자 메뉴] ================");
            System.out.println(" 1. 메뉴 관리 (등록/수정/사진)");
            System.out.println(" 2. 가격 책정 (분식/일반)");
            System.out.println(" 3. 쿠폰 정책 관리");
            System.out.println(" 4. 통계 및 보고서");
            System.out.println(" 5. 데이터 관리 (CSV)");
            System.out.println(" 0. 로그아웃");
            System.out.print("선택>> ");

            int choice = getIntInput();
            try {
                switch (choice) {
                    case 1: handleMenuManagement(); break;
                    case 2: handlePriceManagement(); break;
                    case 3: handleCouponPolicy(); break;
                    case 4: handleReports(); break;
                    case 5: handleDataManagement(); break;
                    case 0:
                        System.out.println("로그아웃 되었습니다.");
                        currentUser = null;
                        return;
                    default: System.out.println("잘못된 선택입니다.");
                }
            } catch (Exception e) {
                System.out.println("⚠️ 에러: " + e.getMessage());
            }
        }
    }

    // --- 관리자 하위 메뉴 ---

    private static void handleMenuManagement() throws IOException {
        while (true) {
            System.out.println("\n--- [관리자 > 메뉴 관리] ---");
            System.out.println(" 1. 메뉴 신규 등록");
            System.out.println(" 2. 메뉴 정보 수정 (이름/가격)");
            System.out.println(" 3. 메뉴 사진 등록");
            System.out.println(" 0. 뒤로가기");
            System.out.print("선택>> ");
            int c = getIntInput();
            if (c == 0) return;
            switch (c) {
                case 1: testMenuInsert(); break;
                case 2: testMenuUpdate(); break;
                case 3: testMenuImageRegister(); break;
                default: System.out.println("잘못된 선택");
            }
        }
    }

    private static void handlePriceManagement() throws IOException {
        while (true) {
            System.out.println("\n--- [관리자 > 가격 책정] ---");
            System.out.println(" 1. 분식당 개별 가격 등록");
            System.out.println(" 2. 일반식당(학식/교직원) 일괄 가격 등록");
            System.out.println(" 0. 뒤로가기");
            System.out.print("선택>> ");
            int c = getIntInput();
            if (c == 0) return;
            switch (c) {
                case 1: testPriceRegister(ProtocolCode.PRICE_REGISTER_SNACK_REQUEST); break;
                case 2: testPriceRegister(ProtocolCode.PRICE_REGISTER_REGULAR_REQUEST); break;
                default: System.out.println("잘못된 선택");
            }
        }
    }

    private static void handleCouponPolicy() throws IOException {
        while (true) {
            System.out.println("\n--- [관리자 > 쿠폰 정책] ---");
            System.out.println(" 1. 정책 목록 조회");
            System.out.println(" 2. 신규 정책 생성");
            System.out.println(" 0. 뒤로가기");
            System.out.print("선택>> ");
            int c = getIntInput();
            if (c == 0) return;
            switch (c) {
                case 1: testCouponPolicyList(); break;
                case 2: testCouponPolicyInsert(); break;
                default: System.out.println("잘못된 선택");
            }
        }
    }

    private static void handleReports() throws IOException {
        while (true) {
            System.out.println("\n--- [관리자 > 통계/보고서] ---");
            System.out.println(" 1. 식당별 결제 내역 상세");
            System.out.println(" 2. 식당별 매출 현황");
            System.out.println(" 3. 시간대별 이용 통계");
            System.out.println(" 0. 뒤로가기");
            System.out.print("선택>> ");
            int c = getIntInput();
            if (c == 0) return;
            switch (c) {
                case 1: testOrderPaymentHistory(); break;
                case 2: testSalesReport(); break;
                case 3: testUsageReport(); break;
                default: System.out.println("잘못된 선택");
            }
        }
    }

    private static void handleDataManagement() throws IOException {
        while (true) {
            System.out.println("\n--- [관리자 > CSV 데이터] ---");
            System.out.println(" 1. 샘플 파일 다운로드");
            System.out.println(" 2. 메뉴 일괄 업로드 (CSV)");
            System.out.println(" 0. 뒤로가기");
            System.out.print("선택>> ");
            int c = getIntInput();
            if (c == 0) return;
            switch (c) {
                case 1: testCsvSampleDownload(); break;
                case 2: testCsvUpload(); break;
                default: System.out.println("잘못된 선택");
            }
        }
    }

    // ===============================================================
    // [기능 구현 메서드]
    // ===============================================================

    // 1. 로그인 (필수)
    private static boolean login() throws IOException {
        System.out.println("\n=== [로그인 정보 입력] ===");
        System.out.print("ID: ");
        String id = sc.nextLine();
        System.out.print("PW: ");
        String pw = sc.nextLine();

        UserDTO user = new UserDTO();
        user.setLoginId(id);
        user.setPassword(pw);

        send(new Protocol(ProtocolType.REQUEST, ProtocolCode.LOGIN_REQUEST, user));
        Protocol res = receive();

        if (res.getCode() == ProtocolCode.LOGIN_RESPONSE) {
            currentUser = (UserDTO) res.getData();
            System.out.println("✅ 로그인 성공! (" + currentUser.getUserId() + "님 환영합니다)" + "권한: " + currentUser.getUserType());
            return true;
        } else {
            System.out.println("❌ 로그인 실패: 아이디 또는 비밀번호를 확인하세요.");
            return false;
        }
    }

    // [헬퍼] 식당 목록을 서버에서 받아와서 선택하게 하는 메서드
    private static RestaurantDTO selectRestaurant() throws IOException {
        // 1. 식당 목록 요청
        send(new Protocol(ProtocolType.REQUEST, ProtocolCode.RESTAURANT_LIST_REQUEST, null));
        Protocol res = receive();

        if (res.getCode() == ProtocolCode.RESTAURANT_LIST_RESPONSE) {
            List<RestaurantDTO> list = (List<RestaurantDTO>) res.getData();
            System.out.println("\n--- [식당 선택] ---");
            for (RestaurantDTO r : list) {
                System.out.printf("[%d] %s\n", r.getRestaurantId(), r.getName());
                System.out.printf("    운영시간1: %s ~ %s\n", r.getOpenTime1(), r.getCloseTime1());
                System.out.printf("    운영시간2: %s ~ %s\n", r.getOpenTime2(), r.getCloseTime2());
            }
            System.out.print("식당 ID 선택>> ");
            int id = getIntInput();
            return list.stream().filter(r -> r.getRestaurantId() == id).findFirst().orElse(null);
        } else {
            System.out.println("❌ 식당 목록 조회 실패 (Server Code 0x" + Integer.toHexString(res.getCode()) + ")");
            return null;
        }
    }

    // [헬퍼] 식당 타입에 따라 시간대 선택 ("상시" or "1/2")
    private static String selectMealTime(RestaurantDTO r) {
        // 이름에 'snack'이나 '분식'이 포함되면 상시 운영으로 간주
        if (r.getName().toLowerCase().contains("snack") || r.getName().contains("분식")) {
            System.out.println(">> '상시' 운영 식당입니다.");
            return "상시";
        }
        // 그 외(학생, 교직원)는 시간 선택
        else {
            System.out.println("--- [시간대 선택] ---");
            System.out.println(" 1. 운영시간1 (" + r.getOpenTime1() + " ~ " + r.getCloseTime1() + ")");
            System.out.println(" 2. 운영시간2 (" + r.getOpenTime2() + " ~ " + r.getCloseTime2() + ")");
            System.out.print("선택>> ");
            int c = getIntInput();
            if (c == 1) return "운영시간1";
            if (c == 2) return "운영시간2";
            return ""; // 잘못된 선택
        }
    }

    // --- 사용자 기능 ---
    // 0x03: 메뉴 목록 조회
    private static void testMenuList() throws IOException {
        RestaurantDTO r = selectRestaurant();
        if (r == null) return;

        String time = selectMealTime(r);
        if (time.isEmpty()) {
            System.out.println("잘못된 시간대 선택입니다.");
            return;
        }

        MenuPriceDTO req = new MenuPriceDTO();
        req.setRestaurantId(r.getRestaurantId());
        req.setMealTime(time);

        send(new Protocol(ProtocolType.REQUEST, ProtocolCode.MENU_LIST_REQUEST, req));
        Protocol res = receive();
        if (res.getCode() == ProtocolCode.MENU_LIST_RESPONSE) {
            List<MenuPriceDTO> list = (List<MenuPriceDTO>) res.getData();
            System.out.println("\n📋 [" + r.getName() + " - " + time + "] 메뉴 목록:");
            if (list.isEmpty()) System.out.println("   (판매 중인 메뉴가 없습니다)");
            for (MenuPriceDTO m : list) {
                System.out.printf("- [%d] %s (학생:%d원 / 직원:%d원)\n",
                        m.getMenuPriceId(), m.getMenuName(), m.getPriceStu(), m.getPriceFac());
            }
        } else printFail(res);
    }

    // 0x04: 메뉴 이미지 다운로드
    private static void testMenuImageDownload() throws IOException {
        System.out.println("\n[메뉴 이미지 다운로드]");
        System.out.print("다운로드할 메뉴 ID: ");
        int menuId = getIntInput();
        send(new Protocol(ProtocolType.REQUEST, ProtocolCode.MENU_IMAGE_DOWNLOAD_REQUEST, menuId));
        Protocol res = receive();
        if (res.getCode() == ProtocolCode.MENU_IMAGE_RESPONSE) {
            byte[] imgData = (byte[]) res.getData();
            String fileName = "downloaded_menu_" + menuId + ".jpg";
            Files.write(Paths.get(fileName), imgData);
            System.out.println("✅ 이미지 다운로드 완료 (" + imgData.length + " bytes) -> " + fileName);
        } else {
            printFail(res);
        }
    }

    // 0x05: 쿠폰 목록 조회
    private static void testCouponList() throws IOException {
        send(new Protocol(ProtocolType.REQUEST, ProtocolCode.COUPON_LIST_REQUEST, currentUser.getUserId()));
        Protocol res = receive();
        if (res.getCode() == ProtocolCode.COUPON_LIST_RESPONSE) {
            List<CouponDTO> list = (List<CouponDTO>) res.getData();
            System.out.println("🎟️ 내 쿠폰 목록:");
            System.out.println("보유 쿠폰 (" + list.size() + "장):");
            for(CouponDTO c : list) {
                System.out.printf("- ID:%d, 가액:%d원, 구매일:%s\n", c.getCouponId(), c.getPurchaseValue(), c.getPurchaseDate());
            }
        } else printFail(res);
    }

    // 0x06: 쿠폰 구매
    private static void testCouponPurchase() throws IOException {
        System.out.println("\n[쿠폰 구매]");
        Map<String, Integer> req = new HashMap<>();
        req.put("userId", currentUser.getUserId());
        System.out.print("구매 수량: ");
        req.put("quantity", getIntInput());

        send(new Protocol(ProtocolType.REQUEST, ProtocolCode.COUPON_PURCHASE_REQUEST, req));
        printSimpleResult(receive());
    }

    // 0x07, 0x08: 결제 (카드/쿠폰)
    private static void testPayment(byte code) throws IOException {
        System.out.println(code == ProtocolCode.PAYMENT_CARD_REQUEST ? "\n[카드 결제]" : "\n[쿠폰 결제]");

        // 1. 식당 선택 및 시간대 선택
        RestaurantDTO r = selectRestaurant();
        if (r == null) return;

        String time = selectMealTime(r);
        if (time.isEmpty()) return;

        // 2. 해당 조건의 메뉴 목록 조회 (내부적으로)
        MenuPriceDTO req = new MenuPriceDTO();
        req.setRestaurantId(r.getRestaurantId());
        req.setMealTime(time);

        send(new Protocol(ProtocolType.REQUEST, ProtocolCode.MENU_LIST_REQUEST, req));
        Protocol menuRes = receive();

        MenuPriceDTO selectedMenu = null;
        if (menuRes.getCode() == ProtocolCode.MENU_LIST_RESPONSE) {
            List<MenuPriceDTO> list = (List<MenuPriceDTO>) menuRes.getData();
            if (list.isEmpty()) {
                System.out.println("❌ 해당 시간대에 판매 중인 메뉴가 없습니다.");
                return;
            }
            System.out.println("--- [메뉴 선택] ---");
            for (MenuPriceDTO m : list) {
                System.out.printf("[%d] %s\n", m.getMenuPriceId(), m.getMenuName());
            }
            System.out.print("메뉴 ID 입력: ");
            int mid = getIntInput();
            selectedMenu = list.stream().filter(m->m.getMenuPriceId()==mid).findFirst().orElse(null);
        } else {
            printFail(menuRes);
            return;
        }

        if (selectedMenu == null) {
            System.out.println("❌ 잘못된 메뉴 ID");
            return;
        }

        int price = currentUser.getUserType().equals("교직원") ? selectedMenu.getPriceFac() : selectedMenu.getPriceStu();
        System.out.println(">> 선택 메뉴: " + selectedMenu.getMenuName());
        System.out.println(">> 결제 금액: " + price + "원");

        // 3. 결제 객체 생성
        PaymentDTO pay = new PaymentDTO();
        pay.setUserId(currentUser.getUserId());
        pay.setUserType(currentUser.getUserType());
        pay.setMenuPriceId(selectedMenu.getMenuPriceId());

        // 4. 쿠폰 처리
        int couponValue = 0;
        if (code == ProtocolCode.PAYMENT_COUPON_REQUEST) {
            System.out.print("사용할 쿠폰 ID: ");
            int cid = getIntInput();
            pay.setUsedCouponId(cid);
            couponValue = findCouponValue(cid);
            if (couponValue < 0) {
                System.out.println("❌ 유효하지 않은 쿠폰");
                return;
            }
            System.out.println(">> 쿠폰 차감: -" + couponValue + "원");
        }

        // 5. 최종 확인
        int extra = (price > couponValue) ? price - couponValue : 0;
        System.out.println("--------------------------------");
        System.out.println(" 최종 결제액(카드): " + extra + "원");
        System.out.println("--------------------------------");
        System.out.print("결제하시겠습니까? (Y/N): ");
        if (!sc.nextLine().equalsIgnoreCase("Y")) return;

        // 6. 전송
        send(new Protocol(ProtocolType.REQUEST, code, pay));
        Protocol res = receive();

        if (res.getCode() == ProtocolCode.SUCCESS) {
            System.out.println("✅ 결제 성공!");
            if (res.getData() instanceof PaymentDTO) {
                PaymentDTO result = (PaymentDTO) res.getData();
                System.out.println("   [영수증]");
                System.out.println("   ★ 주문번호: " + result.getPaymentId());
                System.out.println("   - 메뉴: " + result.getMenuName());
                System.out.println("   - 상태: " + result.getStatus());
                System.out.println("   - 총액: " + result.getMenuPriceAtTime() + "원");
                if (result.getCouponValueUsed() > 0)
                    System.out.println("   - 쿠폰: -" + result.getCouponValueUsed() + "원");
                System.out.println("   - 카드: " + result.getAdditionalCardAmount() + "원");
                System.out.println("   - 시간: " + result.getPaymentTime());
            }
        } else {
            printFail(res);
        }
    }

    private static MenuPriceDTO findMenuInfo(int restaurantId) throws IOException {
        // 시간대는 테스트 편의상 '점심'으로 고정하거나 사용자에게 입력받을 수 있음.
        // 여기서는 편의상 사용자가 입력하도록 함
        System.out.println("시간대 선택 (1:운영시간1, 2:운영시간2, 0:상시): ");
        int t = getIntInput();
        String time = (t == 1) ? "운영시간1" : (t == 2) ? "운영시간2" : "상시";

        MenuPriceDTO req = new MenuPriceDTO();
        req.setRestaurantId(restaurantId);
        req.setMealTime(time);

        send(new Protocol(ProtocolType.REQUEST, ProtocolCode.MENU_LIST_REQUEST, req));
        Protocol res = receive();

        if (res.getCode() == ProtocolCode.MENU_LIST_RESPONSE) {
            List<MenuPriceDTO> list = (List<MenuPriceDTO>) res.getData();
            if (list.isEmpty()) {
                System.out.println("❌ 해당 조건의 메뉴가 없습니다.");
                return null;
            }
            System.out.println("--- [판매 중인 메뉴] ---");
            for (MenuPriceDTO m : list) {
                System.out.printf("[%d] %s (%d원)\n", m.getMenuPriceId(), m.getMenuName(),
                        currentUser.getUserType().equals("교직원") ? m.getPriceFac() : m.getPriceStu());
            }
            System.out.print("메뉴 ID 선택: ");
            int selectedId = getIntInput();
            return list.stream().filter(m -> m.getMenuPriceId() == selectedId).findFirst().orElse(null);
        }
        return null;
    }

    // [헬퍼] 쿠폰 가치를 찾기 위해 목록을 조회하는 메서드
    private static int findCouponValue(int couponId) throws IOException {
        send(new Protocol(ProtocolType.REQUEST, ProtocolCode.COUPON_LIST_REQUEST, currentUser.getUserId()));
        Protocol res = receive();

        if (res.getCode() == ProtocolCode.COUPON_LIST_RESPONSE) {
            List<CouponDTO> list = (List<CouponDTO>) res.getData();
            for (CouponDTO c : list) {
                if (c.getCouponId() == couponId) {
                    return c.getPurchaseValue();
                }
            }
        }
        return -1; // 쿠폰 없음 or 내 거 아님
    }

    // 0x09: 이용 내역 조회
    private static void testUsageHistory() throws IOException {
        send(new Protocol(ProtocolType.REQUEST, ProtocolCode.USAGE_HISTORY_REQUEST, currentUser.getUserId()));
        Protocol res = receive();
        if(res.getCode() == ProtocolCode.USAGE_HISTORY_RESPONSE) {
            List<PaymentDTO> list = (List<PaymentDTO>) res.getData();
            System.out.println("📜 이용 내역:");
            for(PaymentDTO p : list) System.out.printf("- %s (%d원) %s\n", p.getMenuName(), p.getMenuPriceAtTime(), p.getPaymentTime());
        } else printFail(res);
    }

    // 0x0A: 쿠폰 구매 내역 조회
    private static void testCouponPurchaseHistory() throws IOException {
        send(new Protocol(ProtocolType.REQUEST, ProtocolCode.COUPON_PURCHASE_HISTORY_REQUEST, currentUser.getUserId()));
        Protocol res = receive();

        if (res.getCode() == ProtocolCode.COUPON_PURCHASE_HISTORY_RESPONSE) {
            List<CouponDTO> list = (List<CouponDTO>) res.getData();
            System.out.println("📜 쿠폰 구매 이력 (" + list.size() + "건):");
            for (CouponDTO c : list) {
                String status = c.isUsed() ? "[사용됨]" : "[보유중]";
                System.out.printf("- %s %d원권 (구매일: %s)\n", status, c.getPurchaseValue(), c.getPurchaseDate());
            }
        } else {
            printFail(res);
        }
    }

    // --- 관리자 기능 ---

    // 0x10: 메뉴 등록
    private static void testMenuInsert() throws IOException {
        System.out.println("[메뉴 등록]");
        RestaurantDTO r = selectRestaurant();
        if(r == null) return;

        MenuPriceDTO m = new MenuPriceDTO();
        m.setRestaurantId(r.getRestaurantId());
        m.setRestaurantName(r.getName()); // 이름도 세팅 권장

        System.out.print("메뉴명: "); m.setMenuName(sc.nextLine());

        // 시간대 선택 (분식이면 자동 상시, 아니면 선택)
        String time = selectMealTime(r);
        m.setMealTime(time);

        System.out.print("학기명: "); m.setSemesterName(sc.nextLine());
        m.setCurrentSemester(true);
        System.out.print("학생가: "); m.setPriceStu(getIntInput());
        System.out.print("직원가: "); m.setPriceFac(getIntInput());

        System.out.print("날짜(YYYY-MM-DD, 없으면 엔터): ");
        String d = sc.nextLine();
        if(!d.isBlank()) {
            try { m.setDate(LocalDate.parse(d).atStartOfDay()); }
            catch(Exception e){ System.out.println("날짜 오류"); return; }
        }
        send(new Protocol(ProtocolType.REQUEST, ProtocolCode.MENU_INSERT_REQUEST, m));
        printSimpleResult(receive());
    }

    // 0x11: 메뉴 수정
    private static void testMenuUpdate() throws IOException {
        System.out.println("\n[관리자: 메뉴 수정]");
        System.out.println("※ 주의: 수정할 메뉴의 ID를 정확히 입력해야 합니다.");

        MenuPriceDTO m = new MenuPriceDTO();
        System.out.print("수정할 메뉴 ID: ");
        m.setMenuPriceId(getIntInput());

        System.out.print("새 메뉴명: ");
        m.setMenuName(sc.nextLine());

        System.out.print("새 학생가: ");
        m.setPriceStu(getIntInput());

        System.out.print("새 교직원가: ");
        m.setPriceFac(getIntInput());

        send(new Protocol(ProtocolType.REQUEST, ProtocolCode.MENU_UPDATE_REQUEST, m));
        printSimpleResult(receive());
    }

    // 0x12: 메뉴 사진 등록
    private static void testMenuImageRegister() throws IOException {
        System.out.println("\n[관리자: 메뉴 사진 등록]");
        MenuPriceDTO m = new MenuPriceDTO();
        System.out.print("메뉴 ID: "); m.setMenuPriceId(getIntInput());
        System.out.print("업로드할 파일 경로(예: C:\\test.jpg): ");
        String path = sc.nextLine();

        try {
            byte[] fileBytes = Files.readAllBytes(Paths.get(path));
            m.setImageBytes(fileBytes);
            m.setUploadFileName(Paths.get(path).getFileName().toString());

            send(new Protocol(ProtocolType.REQUEST, ProtocolCode.MENU_PHOTO_REGISTER_REQUEST, m));
            printSimpleResult(receive());
        } catch (Exception e) {
            System.out.println("❌ 파일 읽기 실패: " + e.getMessage());
        }
    }

    // 0x13, 0x14: 가격 등록
    private static void testPriceRegister(byte code) throws IOException {
        System.out.println("\n[관리자: 가격 등록 (" + (code==0x13?"분식":"일괄") + ")]");
        MenuPriceDTO m = new MenuPriceDTO();
        System.out.print("식당 ID: "); m.setRestaurantId(getIntInput());
        System.out.print("학기명: "); m.setSemesterName(sc.nextLine());
        m.setCurrentSemester(true);
        System.out.print("학생가: "); m.setPriceStu(getIntInput());
        System.out.print("교직원가: "); m.setPriceFac(getIntInput());

        if (code == ProtocolCode.PRICE_REGISTER_SNACK_REQUEST) {
            System.out.print("메뉴명: "); m.setMenuName(sc.nextLine());
            System.out.print("식당명: "); m.setRestaurantName(sc.nextLine());
            System.out.print("시간대: "); m.setMealTime(sc.nextLine());
        }

        send(new Protocol(ProtocolType.REQUEST, code, m));
        printSimpleResult(receive());
    }

    // 0x15: 쿠폰 정책 목록
    private static void testCouponPolicyList() throws IOException {
        send(new Protocol(ProtocolType.REQUEST, ProtocolCode.COUPON_POLICY_LIST_REQUEST, null));
        Protocol res = receive();
        if (res.getCode() == ProtocolCode.COUPON_POLICY_LIST_RESPONSE) {
            List<CouponPolicyDTO> list = (List<CouponPolicyDTO>) res.getData();
            System.out.println("📜 쿠폰 정책 목록:");
            for (CouponPolicyDTO p : list) System.out.println("- 가격: " + p.getCouponPrice() + ", 적용일: " + p.getEffectiveDate());
        } else printFail(res);
    }

    // 0x16: 쿠폰 정책 생성
    private static void testCouponPolicyInsert() throws IOException {
        System.out.println("\n[관리자: 쿠폰 정책 생성]");
        CouponPolicyDTO p = new CouponPolicyDTO();
        System.out.print("쿠폰 가격: "); p.setCouponPrice(getIntInput());
        p.setEffectiveDate(LocalDateTime.now());

        send(new Protocol(ProtocolType.REQUEST, ProtocolCode.COUPON_POLICY_INSERT_REQUEST, p));
        printSimpleResult(receive());
    }

    // 0x17: 주문 결제 내역 조회
    private static void testOrderPaymentHistory() throws IOException {
        System.out.println("\n[관리자: 식당별 결제 내역 조회]");
        System.out.print("식당 ID: ");
        int rId = getIntInput();
        send(new Protocol(ProtocolType.REQUEST, ProtocolCode.ORDER_PAYMENT_HISTORY_REQUEST, rId));

        Protocol res = receive();
        if(res.getCode() == ProtocolCode.ORDER_PAYMENT_HISTORY_RESPONSE) {
            List<PaymentDTO> list = (List<PaymentDTO>) res.getData();
            System.out.println("📜 결제 내역 (" + list.size() + "건):");
            for(PaymentDTO p : list) System.out.println("- " + p.getMenuName() + ", " + p.getMenuPriceAtTime() + "원");
        } else printFail(res);
    }

    // 0x18: 매출 현황
    private static void testSalesReport() throws IOException {
        send(new Protocol(ProtocolType.REQUEST, ProtocolCode.SALES_REPORT_REQUEST, null));
        Protocol res = receive();
        if(res.getCode() == ProtocolCode.SALES_REPORT_RESPONSE) {
            Map<String, Long> sales = (Map<String, Long>) res.getData();
            System.out.println("💰 식당별 매출: " + sales);
        } else printFail(res);
    }

    // 0x19: 이용 현황
    private static void testUsageReport() throws IOException {
        send(new Protocol(ProtocolType.REQUEST, ProtocolCode.USAGE_REPORT_REQUEST, null));
        Protocol res = receive();
        if(res.getCode() == ProtocolCode.TIME_STATS_RESPONSE) {
            List<String> stats = (List<String>) res.getData();
            System.out.println("📊 시간대별 통계:");
            stats.forEach(System.out::println);
        } else printFail(res);
    }

    // 0x20: CSV 샘플 다운로드
    private static void testCsvSampleDownload() throws IOException {
        send(new Protocol(ProtocolType.REQUEST, ProtocolCode.CSV_SAMPLE_DOWNLOAD_REQUEST, null));
        Protocol res = receive();
        if(res.getCode() == ProtocolCode.CSV_FILE_RESPONSE) {
            byte[] data = (byte[]) res.getData();
            Files.write(Paths.get("sample.csv"), data);
            System.out.println("✅ sample.csv 다운로드 완료");
        } else printFail(res);
    }

    // 0x21: CSV 업로드
    private static void testCsvUpload() throws IOException {
        System.out.println("\n[관리자: CSV 업로드]");
        System.out.print("업로드할 CSV 파일 경로: ");
        String path = sc.nextLine();
        try {
            byte[] data = Files.readAllBytes(Paths.get(path));
            send(new Protocol(ProtocolType.REQUEST, ProtocolCode.CSV_MENU_UPLOAD_REQUEST, data));
            printSimpleResult(receive());
        } catch (Exception e) {
            System.out.println("파일 에러: " + e.getMessage());
        }
    }

    // ===============================================================
    // [유틸리티 메서드]
    // ===============================================================

    private static void send(Protocol p) throws IOException {
        os.write(p.getBytes());
        os.flush();
    }

    private static Protocol receive() throws IOException {
        // 1. 헤더 읽기
        byte[] header = new byte[Protocol.HEADER_SIZE];
        int totalRead = 0;
        while (totalRead < Protocol.HEADER_SIZE) {
            int r = is.read(header, totalRead, Protocol.HEADER_SIZE - totalRead);
            if (r == -1) throw new IOException("서버 연결 끊김");
            totalRead += r;
        }

        // 2. 길이 확인
        int len = java.nio.ByteBuffer.wrap(header, 2, 4).getInt();

        // 3. 바디 읽기
        byte[] body = new byte[len];
        totalRead = 0;
        while (totalRead < len) {
            int r = is.read(body, totalRead, len - totalRead);
            if (r == -1) throw new IOException("서버 연결 끊김");
            totalRead += r;
        }

        // 4. 합치기
        byte[] packet = new byte[Protocol.HEADER_SIZE + len];
        System.arraycopy(header, 0, packet, 0, Protocol.HEADER_SIZE);
        if (len > 0) System.arraycopy(body, 0, packet, Protocol.HEADER_SIZE, len);

        return new Protocol(packet);
    }

    private static int getIntInput() {
        try {
            int i = Integer.parseInt(sc.nextLine());
            return i;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static void printSimpleResult(Protocol res) {
        if (res.getCode() == ProtocolCode.SUCCESS) {
            System.out.println("✅ 성공 (SUCCESS)");
        } else if (res.getCode() == ProtocolCode.FAIL) {
            System.out.println("❌ 실패 (FAIL): " + res.getData());
        } else if (res.getCode() == ProtocolCode.PERMISSION_DENIED) {
            System.out.println("⛔ 권한 없음 (PERMISSION_DENIED)");
        } else {
            System.out.println("⚠️ 기타 응답 코드: 0x" + Integer.toHexString(res.getCode()));
        }
    }

    private static void printFail(Protocol res) {
        System.out.println("❌ 요청 실패: Code=0x" + Integer.toHexString(res.getCode()) + ", Data=" + res.getData());
    }

    private static void close() {
        try { if(socket != null) socket.close(); } catch(Exception e) {}
    }
}