package server;

import controller.MenuController;
import controller.CouponController;
import controller.PriceController;
import controller.PaymentController;
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import network.Protocol;
import network.ProtocolCode;
import network.ProtocolType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import persistence.dao.MenuPriceDAO;
import persistence.dao.PaymentDAO;
import persistence.dao.RestaurantDAO;
import persistence.dao.UserDAO;
import persistence.dto.*;

public class ClientHandler extends Thread {
    private static final Log log = LogFactory.getLog(ClientHandler.class);
    private final Socket clientSocket;

    // 컨트롤러 및 DAO 초기화
    private final MenuController menuController = new MenuController();
    private final CouponController couponController = new CouponController();
    private final PaymentController paymentController = new PaymentController();
    private final PriceController priceController = new PriceController();
    private final UserDAO userDAO = new UserDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();

    private UserDTO loginUser = null;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (InputStream is = clientSocket.getInputStream();
             OutputStream os = clientSocket.getOutputStream()) {

            // 연결이 유지되는 동안 계속 요청을 처리 (Persistent Connection)
            while (true) {
                byte[] data;
                try {
                    data = readProtocol(is);
                } catch (IOException e) {
                    System.out.println("클라이언트 연결 종료: " + clientSocket.getInetAddress());
                    break;
                }

                if (data == null) break;

                Protocol request = new Protocol(data);
                System.out.println("요청 수신: 0x" + Integer.toHexString(request.getCode() & 0xFF).toUpperCase());

                // 요청 처리 및 응답 생성
                Protocol response = handleRequest(request);

                // 응답 전송
                os.write(response.getBytes());
                os.flush();
            }
        } catch (Exception e) {
            System.err.println("클라이언트 핸들러 오류: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            } catch (IOException e) {}
        }
    }

    // 데이터 수신 헬퍼 메서드
    private byte[] readProtocol(InputStream is) throws IOException {
        byte[] header = new byte[Protocol.HEADER_SIZE];
        int totalRead = 0;

        while (totalRead < Protocol.HEADER_SIZE) {
            int read = is.read(header, totalRead, Protocol.HEADER_SIZE - totalRead);
            if (read == -1) {
                throw new IOException("EOF"); // 클라이언트 연결 종료
            }
            totalRead += read;
        }

        // 2. 데이터 길이 파악 (헤더의 2~5번째 바이트가 길이 정보)
        int dataLength = java.nio.ByteBuffer.wrap(header, 2, 4).getInt();

        // 3. 데이터 본문(Body) 끝까지 읽기
        byte[] body = new byte[dataLength];
        totalRead = 0;
        while (totalRead < dataLength) {
            int read = is.read(body, totalRead, dataLength - totalRead);
            if (read == -1) throw new IOException("EOF");
            totalRead += read;
        }

        // 4. 전체 패킷 합치기 (Protocol 생성자에게 넘겨주기 위함)
        byte[] packet = new byte[Protocol.HEADER_SIZE + dataLength];
        System.arraycopy(header, 0, packet, 0, Protocol.HEADER_SIZE);
        if (dataLength > 0) {
            System.arraycopy(body, 0, packet, Protocol.HEADER_SIZE, dataLength);
        }

        return packet;
    }

    // 핵심 로직: 프로토콜 코드에 따른 분기 처리
    private Protocol handleRequest(Protocol req) {
        if (req.getType() != ProtocolType.REQUEST) {
            return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, null);
        }
        // [추가] 권한 체크 로직 (관리자 기능 접근 제어)
        // ProtocolCode 0x10 ~ 0x29 범위는 관리자 전용이라고 가정
        if (req.getCode() >= 0x10 && req.getCode() <= 0x29) {
            if (this.loginUser == null ||
                    !("admin".equalsIgnoreCase(this.loginUser.getUserType()) || "관리자".equals(this.loginUser.getUserType()))) {
                // 0x55: PERMISSION_DENIED 반환
                return new Protocol(ProtocolType.RESULT, ProtocolCode.PERMISSION_DENIED, "관리자 권한이 필요합니다.");
            }
        }
        try {
            switch (req.getCode()) {
                // ==========================================
                // I. 사용자 인증 및 결제
                // ==========================================
                case ProtocolCode.LOGIN_REQUEST: { // 0x02
                    UserDTO u = (UserDTO) req.getData();
                    UserDTO result = userDAO.findUserByLoginId(u.getLoginId(), u.getPassword());
                    if (result != null) {
                        this.loginUser = result; // 세션에 로그인 사용자 저장
                        // 성공 시 LOGIN_RESPONSE (0x30) + 유저 데이터 반환
                        loginUser = result;
                        return new Protocol(ProtocolType.RESPONSE, ProtocolCode.LOGIN_RESPONSE, result);
                    } else {
                        // 실패 시 INVALID_INPUT (0x52) 반환
                        return new Protocol(ProtocolType.RESULT, ProtocolCode.INVALID_INPUT, null);
                    }
                }

                case ProtocolCode.MENU_LIST_REQUEST: { // 0x03
                    Object data = req.getData();
                    int restId = 1;
                    String mealTime = null;
                    String menuDate = null;
                    Integer menuPriceId = null;

                    if (data instanceof String s) {
                        restId = mapRestaurantCodeToId(s);
                    } else if (data instanceof Integer i) {
                        menuPriceId = i;
                    } else if (data instanceof Map<?, ?> map) {
                        Object rid = map.get("restaurantId");
                        if (rid instanceof Integer) restId = (Integer) rid;
                        Object mt = map.get("mealTime");
                        if (mt instanceof String) mealTime = (String) mt;
                        Object md = map.get("menuDate");
                        if (md instanceof String) menuDate = (String) md;
                    } else if (data instanceof MenuPriceDTO dto) {
                        restId = dto.getRestaurantId();
                        mealTime = dto.getMealTime();
                        menuDate = dto.getMenuDate();
                    } else {
                        return new Protocol(ProtocolType.RESULT, ProtocolCode.INVALID_INPUT, null);
                    }

                    // 단건 상세 요청 (order 화면 재사용)
                    if (menuPriceId != null && menuPriceId > 0) {
                        MenuPriceDTO menu = menuController.getMenuById(menuPriceId);
                        if (menu == null) {
                            return new Protocol(ProtocolType.RESULT, ProtocolCode.NOT_FOUND, null);
                        }
                        return new Protocol(ProtocolType.RESPONSE, ProtocolCode.MENU_LIST_RESPONSE, menu);
                    }

                    List<MenuPriceDTO> menus;
                    if (menuDate != null && !menuDate.isBlank()) {
                        menus = menuController.getMenusByRestaurantAndDate(restId, menuDate);
                    } else if (mealTime != null && !mealTime.isBlank()) {
                        menus = menuController.getMenus(restId, mealTime);
                    } else {
                        menus = menuController.getMenusByRestaurant(restId);
                    }
                    return new Protocol(ProtocolType.RESPONSE, ProtocolCode.MENU_LIST_RESPONSE, menus);
                }

                case ProtocolCode.MENU_IMAGE_DOWNLOAD_REQUEST: { // 0x04
                    int menuId = (int) req.getData();
                    byte[] img = menuController.getMenuImage(menuId);
                    if (img != null) {
                        return new Protocol(ProtocolType.RESPONSE, ProtocolCode.MENU_IMAGE_RESPONSE,  img);
                    } else {
                        return new Protocol(ProtocolType.RESULT, ProtocolCode.NOT_FOUND, null);
                    }
                }
                case ProtocolCode.COUPON_LIST_REQUEST: { // 0x05 (쿠폰 조회)
                    int userId = (int) req.getData();
                    // [연결] 잔여 쿠폰 목록 조회
                    List<CouponDTO> coupons = couponController.getMyCoupons(userId);
                    return new Protocol(ProtocolType.RESPONSE, ProtocolCode.COUPON_LIST_RESPONSE, coupons);
                }

                case ProtocolCode.COUPON_PURCHASE_REQUEST: { // 0x06 (쿠폰 구매)
                    // 클라이언트가 Map이나 DTO로 {userId, quantity}를 보낸다고 가정
                    // 여기서는 편의상 DTO나 Map 처리 예시를 듭니다.
                    // 만약 DTO를 안 쓴다면 Map<String, Integer> 등을 활용하세요.
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Integer> reqData = (java.util.Map<String, Integer>) req.getData();
                    int userId = reqData.get("userId");
                    int quantity = reqData.get("quantity");

                    // [연결] 구매 로직 수행
                    return couponController.purchaseCoupons(userId, quantity);
                }
                case ProtocolCode.PAYMENT_CARD_REQUEST:   // 0x07
                case ProtocolCode.PAYMENT_COUPON_REQUEST: // 0x08
                {
                    // PaymentController에서 카드/쿠폰 구분 로직 처리
                    return paymentController.processPayment((PaymentDTO) req.getData());
                }

                case ProtocolCode.USAGE_HISTORY_REQUEST: { // 0x09
                    int userId = (int) req.getData();
                    List<PaymentDTO> history = paymentDAO.findHistoryByUserId(userId);
                    return new Protocol(ProtocolType.RESPONSE, ProtocolCode.USAGE_HISTORY_RESPONSE,  history);
                }

                case ProtocolCode.COUPON_PURCHASE_HISTORY_REQUEST: { // 0x0A
                    int userId = (int) req.getData();
                    return couponController.getCouponPurchaseHistory(userId);
                }

                // ==========================================
                // II. 관리자 - 메뉴/가격 관리
                // ==========================================
                case ProtocolCode.MENU_INSERT_REQUEST:       // 0x10
                    return menuController.registerOrUpdateMenu((MenuPriceDTO) req.getData());
                case ProtocolCode.MENU_UPDATE_REQUEST:       // 0x11
                {
                    return menuController.registerOrUpdateMenu((MenuPriceDTO) req.getData());
                }

                case ProtocolCode.MENU_PHOTO_REGISTER_REQUEST: // 0x12
                    return menuController.uploadMenuImage((MenuPriceDTO) req.getData());

                case ProtocolCode.PRICE_REGISTER_SNACK_REQUEST: // 0x13 (분식당 단일 메뉴 가격)
                {
                    return priceController.upsertMenuPriceForSemester((MenuPriceDTO) req.getData());
                }

                case ProtocolCode.PRICE_REGISTER_REGULAR_REQUEST: // 0x14 (학식/교직원 일괄 가격)
                {
                    return priceController.bulkUpdatePricesForSemester((MenuPriceDTO) req.getData());
                }

                // ==========================================
                // III. 관리자 - 정책/보고서/CSV
                // ==========================================
                case ProtocolCode.COUPON_POLICY_LIST_REQUEST: // 0x15
                {
                    return couponController.getCouponPolicies();
                }

                case ProtocolCode.COUPON_POLICY_INSERT_REQUEST: // 0x16
                    return couponController.upsertCouponPolicy((CouponPolicyDTO) req.getData());

                case ProtocolCode.ORDER_PAYMENT_HISTORY_REQUEST: { // 0x17
                    Object data = req.getData();
                    Integer restaurantId = null;
                    LocalDateTime start = null;
                    LocalDateTime end = null;

                    if (data instanceof Integer i) {
                        restaurantId = i;
                    } else if (data instanceof Map<?, ?> map) {
                        Object rid = map.get("restaurantId");
                        if (rid instanceof Integer) {
                            restaurantId = (Integer) rid;
                        }
                        Object s = map.get("start");
                        Object e = map.get("end");
                        if (s instanceof String) start = parseDateTime((String) s);
                        if (e instanceof String) end = parseDateTime((String) e);
                    }

                    List<PaymentDTO> list;
                    if (start != null && end != null) {
                        if (restaurantId != null && restaurantId > 0) {
                            list = paymentDAO.findHistoryByRestaurantAndPeriod(restaurantId, start, end);
                        } else {
                            list = paymentDAO.findHistoryByPeriod(start, end);
                        }
                    } else if (restaurantId != null && restaurantId > 0) {
                        list = paymentDAO.findHistoryByRestaurantId(restaurantId);
                    } else {
                        // 필터가 없으면 잘못된 요청으로 처리
                        return new Protocol(ProtocolType.RESULT, ProtocolCode.INVALID_INPUT, "조회 조건이 없습니다.");
                    }

                    return new Protocol(ProtocolType.RESPONSE, ProtocolCode.ORDER_PAYMENT_HISTORY_RESPONSE, list);
                }

                case ProtocolCode.SALES_REPORT_REQUEST: { // 0x18
                    Map<String, Long> sales = paymentDAO.getSalesStatsByRestaurant();
                    return new Protocol(ProtocolType.RESPONSE, ProtocolCode.SALES_REPORT_RESPONSE, sales);
                }

                case ProtocolCode.USAGE_REPORT_REQUEST: { // 0x19
                    List<String> stats = paymentDAO.getTimeSlotUsageStats();
                    // ProtocolCode에 정의된 TIME_STATS_RESPONSE (0x3A) 사용
                    return new Protocol(ProtocolType.RESPONSE, ProtocolCode.TIME_STATS_RESPONSE, stats);
                }

                // CSV 샘플 다운로드 (중복 코드 모두 처리)
                case ProtocolCode.CSV_SAMPLE_DOWNLOAD_REQUEST: // 0x20
                case ProtocolCode.ADMIN_CSV_SAMPLE_REQUEST:    // 0x22
                {
                    byte[] sample = menuController.getCsvSample();
                    return new Protocol(ProtocolType.RESPONSE, ProtocolCode.CSV_FILE_RESPONSE, sample);
                }

                // CSV 업로드 (중복 코드 모두 처리)
                case ProtocolCode.CSV_MENU_UPLOAD_REQUEST:   // 0x21
                case ProtocolCode.ADMIN_CSV_UPLOAD_REQUEST:  // 0x23
                {
                    byte[] csvData = (byte[]) req.getData();
                    return menuController.registerMenuFromCSV(csvData);
                }

                default:
                    System.out.println("알 수 없는 요청 코드: 0x" + Integer.toHexString(req.getCode()).toUpperCase());
                    return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Protocol(ProtocolType.RESULT, ProtocolCode.SERVER_ERROR, null);
        }
    }

    private LocalDateTime parseDateTime(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            return LocalDateTime.parse(text, fmt);
        } catch (DateTimeParseException e) {
            System.err.println("날짜 파싱 실패: " + text);
            return null;
        }
    }

    private int mapRestaurantCodeToId(String code) {
        return switch (code) {
            case "stdCafeteria", "학생식당", "student" -> 1;
            case "facCafeteria", "교직원식당", "faculty" -> 2;
            case "snack", "분식당" -> 3;
            default -> 1;
        };
    }
}
