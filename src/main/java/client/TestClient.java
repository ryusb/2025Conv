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

            System.out.println("🎉 [TestClient] 서버에 연결되었습니다.");

            // 1. 먼저 로그인 수행
            if (!login()) {
                System.out.println("❌ 로그인 실패로 프로그램을 종료합니다.");
                return;
            }

            // 2. 기능 테스트 메뉴 실행
            while (true) {
                printMainMenu();
                int choice = getIntInput();

                if (choice == 0) break;

                try {
                    switch (choice) {
                        // --- 사용자 기능 ---
                        case 1: testMenuList(); break;
                        case 2: testMenuImageDownload(); break;
                        case 3: testCouponList(); break;
                        case 4: testCouponPurchase(); break;
                        case 5: testPayment(ProtocolCode.PAYMENT_CARD_REQUEST); break;
                        case 6: testPayment(ProtocolCode.PAYMENT_COUPON_REQUEST); break;
                        case 7: testUsageHistory(); break;

                        // --- 관리자 기능 ---
                        case 10: testMenuInsert(); break;
                        case 11: testMenuUpdate(); break;
                        case 12: testMenuImageRegister(); break;
                        case 13: testPriceRegister(ProtocolCode.PRICE_REGISTER_SNACK_REQUEST); break;
                        case 14: testPriceRegister(ProtocolCode.PRICE_REGISTER_REGULAR_REQUEST); break;
                        case 15: testCouponPolicyList(); break;
                        case 16: testCouponPolicyInsert(); break;
                        case 17: testOrderPaymentHistory(); break;
                        case 18: testSalesReport(); break;
                        case 19: testUsageReport(); break;
                        case 20: testCsvSampleDownload(); break;
                        case 21: testCsvUpload(); break;

                        default: System.out.println("잘못된 선택입니다.");
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ 테스트 중 에러 발생: " + e.getMessage());
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            System.err.println("❌ 서버 연결 실패: " + e.getMessage());
        } finally {
            close();
        }
    }

    // ===============================================================
    // [기능별 테스트 메서드]
    // ===============================================================

    // 1. 로그인 (필수)
    private static boolean login() throws IOException {
        System.out.println("\n=== [로그인] ===");
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
            System.out.println("✅ 로그인 성공! (권한: " + currentUser.getUserType() + ", ID: " + currentUser.getUserId() + ")");
            return true;
        } else {
            System.out.println("❌ 로그인 실패 (Code: 0x" + Integer.toHexString(res.getCode()) + ")");
            return false;
        }
    }

    // 0x03: 메뉴 목록 조회
    private static void testMenuList() throws IOException {
        System.out.println("\n[메뉴 목록 조회]");
        MenuPriceDTO reqDto = new MenuPriceDTO();
        System.out.print("식당 ID (1:학생, 2:교직원, 3:분식): ");
        reqDto.setRestaurantId(getIntInput());
        System.out.print("시간대 (아침/점심/저녁/상시): ");
        reqDto.setMealTime(sc.nextLine());

        send(new Protocol(ProtocolType.REQUEST, ProtocolCode.MENU_LIST_REQUEST, reqDto));
        Protocol res = receive();

        if (res.getCode() == ProtocolCode.MENU_LIST_RESPONSE) {
            List<MenuPriceDTO> list = (List<MenuPriceDTO>) res.getData();
            System.out.println("📋 메뉴 목록 (" + list.size() + "개):");
            for (MenuPriceDTO m : list) {
                System.out.printf("- [%d] %s (%d원/%d원)\n", m.getMenuPriceId(), m.getMenuName(), m.getPriceStu(), m.getPriceFac());
            }
        } else {
            printFail(res);
        }
    }

    // 0x04: 메뉴 이미지 다운로드
    private static void testMenuImageDownload() throws IOException {
        System.out.println("\n[메뉴 이미지 다운로드]");
        System.out.print("메뉴 ID: ");
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
            System.out.println("🎟️ 보유 쿠폰 (" + list.size() + "장):");
            for(CouponDTO c : list) System.out.println("- " + c.getPurchaseValue() + "원권 (" + c.getPurchaseDate() + ")");
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
        System.out.println("\n[결제 요청 - " + (code == ProtocolCode.PAYMENT_CARD_REQUEST ? "카드" : "쿠폰") + "]");
        PaymentDTO pay = new PaymentDTO();
        pay.setUserId(currentUser.getUserId());
        pay.setUserType(currentUser.getUserType());

        System.out.print("메뉴 ID: ");
        pay.setMenuPriceId(getIntInput());

        if (code == ProtocolCode.PAYMENT_COUPON_REQUEST) {
            System.out.print("사용할 쿠폰 ID: ");
            pay.setUsedCouponId(getIntInput());
        }

        send(new Protocol(ProtocolType.REQUEST, code, pay));
        printSimpleResult(receive());
    }

    // 0x09: 이용 내역 조회
    private static void testUsageHistory() throws IOException {
        send(new Protocol(ProtocolType.REQUEST, ProtocolCode.USAGE_HISTORY_REQUEST, currentUser.getUserId()));
        Protocol res = receive();

        if(res.getCode() == ProtocolCode.USAGE_HISTORY_RESPONSE) {
            List<PaymentDTO> list = (List<PaymentDTO>) res.getData();
            System.out.println("📜 이용 내역:");
            for(PaymentDTO p : list) System.out.println("- " + p.getMenuName() + " (" + p.getPaymentTime() + ") : " + p.getStatus());
        } else printFail(res);
    }

    // --- 관리자 기능 ---

    // 0x10: 메뉴 등록
    private static void testMenuInsert() throws IOException {
        System.out.println("\n[관리자: 메뉴 등록]");
        MenuPriceDTO m = new MenuPriceDTO();
        System.out.print("식당 ID: "); m.setRestaurantId(getIntInput());
        System.out.print("식당 이름: "); m.setRestaurantName(sc.nextLine());
        System.out.print("메뉴명: "); m.setMenuName(sc.nextLine());
        System.out.print("시간대(점심 등): "); m.setMealTime(sc.nextLine());
        System.out.print("학기명: "); m.setSemesterName(sc.nextLine());
        m.setCurrentSemester(true);
        System.out.print("학생가: "); m.setPriceStu(getIntInput());
        System.out.print("교직원가: "); m.setPriceFac(getIntInput());
        m.setDate(LocalDateTime.now()); // 날짜는 현재로 임시 설정

        send(new Protocol(ProtocolType.REQUEST, ProtocolCode.MENU_INSERT_REQUEST, m));
        printSimpleResult(receive());
    }

    // 0x11: 메뉴 수정
    private static void testMenuUpdate() throws IOException {
        System.out.println("\n[관리자: 메뉴 수정]");
        MenuPriceDTO m = new MenuPriceDTO();
        System.out.print("수정할 메뉴 ID: "); m.setMenuPriceId(getIntInput());
        // 필수 정보 입력 (생략 시 에러 날 수 있으므로 입력)
        System.out.print("식당 ID: "); m.setRestaurantId(getIntInput());
        System.out.print("식당 이름: "); m.setRestaurantName(sc.nextLine());
        System.out.print("새 메뉴명: "); m.setMenuName(sc.nextLine());
        System.out.print("시간대: "); m.setMealTime(sc.nextLine());
        System.out.print("학기명: "); m.setSemesterName(sc.nextLine());
        m.setCurrentSemester(true);
        System.out.print("학생가: "); m.setPriceStu(getIntInput());
        System.out.print("교직원가: "); m.setPriceFac(getIntInput());

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

    private static void printMainMenu() {
        System.out.println("\n================ [통합 테스트 메뉴] ================");
        System.out.println(" 1. 메뉴 목록 조회       10. [관리자] 메뉴 등록");
        System.out.println(" 2. 메뉴 이미지 다운     11. [관리자] 메뉴 수정");
        System.out.println(" 3. 쿠폰 목록 조회       12. [관리자] 메뉴 사진 등록");
        System.out.println(" 4. 쿠폰 구매 요청       13. [관리자] 분식당 가격 등록");
        System.out.println(" 5. 카드 결제 요청       14. [관리자] 일괄 가격 등록");
        System.out.println(" 6. 쿠폰 결제 요청       15. [관리자] 쿠폰 정책 목록");
        System.out.println(" 7. 이용 내역 조회       16. [관리자] 쿠폰 정책 생성");
        System.out.println("                        17. [관리자] 결제 내역 조회");
        System.out.println("                        18. [관리자] 매출 현황 조회");
        System.out.println("                        19. [관리자] 이용 통계 조회");
        System.out.println("                        20. [관리자] CSV 샘플 다운");
        System.out.println("                        21. [관리자] CSV 업로드");
        System.out.println(" 0. 종료");
        System.out.print("선택>> ");
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
            System.out.println("--[DEBUG] 권한 체크 시작. 요청 코드: 0x" + Integer.toHexString(res.getCode()));
            if (currentUser == null) {
                System.out.println("--[DEBUG] loginUser가 NULL입니다! (로그인 처리가 안 됨)");
            } else {
                System.out.println("--[DEBUG] 현재 유저: " + currentUser.getLoginId() + ", 타입: " + currentUser.getUserType());
            }
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