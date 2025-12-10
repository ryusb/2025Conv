package server;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import network.Protocol; // Protocol 객체를 사용하여 통신 처리
import network.ProtocolCode;
import network.ProtocolType;
import persistence.dao.CouponDAO;
import persistence.dao.MenuPriceDAO;
import persistence.dao.PaymentDAO;
import persistence.dao.UserDAO; // 예시 DAO 임포트
import persistence.dto.CouponDTO;
import persistence.dto.MenuPriceDTO;
import persistence.dto.PaymentDTO;
import persistence.dto.UserDTO;

public class ClientHandler extends Thread {
    private final Socket clientSocket;
    private final UserDAO userDAO;
    private final MenuPriceDAO menuDAO;
    private final PaymentDAO paymentDAO;
    private final CouponDAO couponDAO;

    // 생성자: 클라이언트 소켓을 받아서 초기화합니다.
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        // DAO 객체 초기화
        this.userDAO = new UserDAO();
        this.menuDAO = new MenuPriceDAO();
        this.paymentDAO = new PaymentDAO();
        this.couponDAO = new CouponDAO();
    }

    @Override
    public void run() {
        // 이 메서드에서 클라이언트와의 통신을 처리합니다.
        try (
                // 클라이언트로부터 데이터를 읽기 위한 InputStream
                InputStream inputStream = clientSocket.getInputStream();
                // 클라이언트에게 데이터를 쓰기 위한 OutputStream
                OutputStream outputStream = clientSocket.getOutputStream();
        ) {
            // 1. 클라이언트로부터 Protocol 객체를 수신
            // (ClientHandler의 핵심 로직)
            byte[] receivedData = readProtocolFromClient(inputStream);

            // Deserializer를 사용하여 Protocol 객체로 변환하는 로직이 필요합니다.
            Protocol receivedProtocol = new Protocol(receivedData);

            System.out.println("수신된 요청 타입: " + receivedProtocol.getType() +
                    ", 코드: " + receivedProtocol.getCode());

            // 2. 요청 처리 (비즈니스 로직)
            Protocol response = handleRequest(receivedProtocol);

            // 3. 응답 전송
            outputStream.write(response.getBytes());
            outputStream.flush();

        } catch (Exception e) {
            System.err.println("클라이언트 처리 중 오류 발생: " + e.getMessage());
            // 예외 발생 시 소켓 닫기
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                    System.out.println("🔗 클라이언트 연결 해제: " + clientSocket.getInetAddress());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // ⚠️ TODO: InputStream에서 바이트 배열을 읽어오는 메서드 구현 필요
    private byte[] readProtocolFromClient(InputStream is) throws IOException {
        // 프로토콜의 전체 길이를 알 수 없으므로, 소켓 버퍼에서 데이터를 읽는 로직이 필요합니다.
        // 일반적으로 4바이트 헤더(전체 길이)를 먼저 읽고, 그 길이만큼 나머지 바디를 읽습니다.
        // 이 부분은 Protocol 구조에 맞게 구현되어야 합니다.
        // 임시로, 단순하게 4096 바이트만 읽는 코드로 대체합니다. (실제로는 수정 필요)
        byte[] buffer = new byte[4096];
        int bytesRead = is.read(buffer);
        if (bytesRead == -1) {
            throw new IOException("클라이언트 연결이 종료되었습니다.");
        }
        return java.util.Arrays.copyOf(buffer, bytesRead);
    }

    // ⚠️ TODO: 수신된 Protocol 객체를 분석하고 응답을 생성하는 메서드 구현 필요
    private Protocol handleRequest(Protocol req) {
        Protocol res = new Protocol(ProtocolType.RESPONSE, ProtocolCode.FAIL, 0, null); // 기본값: 실패
        try {
            switch (req.getCode()) {
                // ============================================================
                // 1. 로그인
                // ============================================================
                case ProtocolCode.LOGIN_REQUEST:
                    UserDTO loginReq = (UserDTO) req.getData();
                    UserDTO user = userDAO.findUserByLoginId(loginReq.getLoginId(), loginReq.getPassword());
                    if (user != null) {
                        res.setCode(ProtocolCode.LOGIN_RESPONSE);
                        res.setData(user);
                    } else {
                        res.setCode(ProtocolCode.INVALID_CREDENTIALS);
                    }
                    break;

                // ============================================================
                // 2. 메뉴 조회 (학생/교직원)
                // ============================================================
                case ProtocolCode.MENU_QUERY_REQUEST:
                    // 클라이언트가 보낸 데이터: "식당ID,식사시간" (String) 또는 DTO
                    // 예시: req.getData()가 String "1,점심" 이라고 가정
                    String[] menuParams = ((String) req.getData()).split(",");
                    int rId = Integer.parseInt(menuParams[0]);
                    String time = menuParams[1];

                    // 오늘 날짜 메뉴 조회 (MenuPriceDAO 업데이트 필요)
                    // 여기서는 편의상 날짜 없이 조회하거나, DAO에 날짜 파라미터 추가 후 사용
                    List<MenuPriceDTO> menus = menuDAO.findCurrentMenus(rId, time);

                    res.setCode(ProtocolCode.MENU_QUERY_RESPONSE);
                    res.setData(menus); // List 직렬화 전송
                    break;

                // ============================================================
                // 3. 결제 요청 (카드/쿠폰)
                // ============================================================
                case ProtocolCode.PAYMENT_REQUEST:
                    PaymentDTO payReq = (PaymentDTO) req.getData();
                    // 비즈니스 로직: 쿠폰 사용 시 유효성 검사 등은 여기서 수행 가능
                    boolean paySuccess = paymentDAO.insertPayment(payReq);

                    if (paySuccess) {
                        res.setCode(ProtocolCode.SUCCESS);
                        // 쿠폰을 사용했다면 쿠폰 상태 업데이트 ("사용됨" 처리)
                        if (payReq.getUsedCouponId() != null) {
                            couponDAO.updateCouponToUsed(payReq.getUsedCouponId());
                        }
                    } else {
                        res.setCode(ProtocolCode.FAIL);
                    }
                    break;

                // ============================================================
                // 4. [기능 1] 개인 이용 내역 조회 (사용자)
                // ============================================================
                case ProtocolCode.USAGE_HISTORY_REQUEST:
                    // 클라이언트가 보낸 UserDTO 또는 userId(int)
                    int userId = (int) req.getData();
                    List<PaymentDTO> myHistory = paymentDAO.findHistoryByUserId(userId);

                    res.setCode(ProtocolCode.USAGE_HISTORY_RESPONSE);
                    res.setData(myHistory);
                    break;

                // ============================================================
                // 5. [기능 2] 식당별 결제 내역 조회 (관리자)
                // ============================================================
                case ProtocolCode.ADMIN_HISTORY_BY_RESTAURANT_REQUEST:
                    int restoId = (int) req.getData();
                    List<PaymentDTO> restoHistory = paymentDAO.findHistoryByRestaurantId(restoId);

                    res.setCode(ProtocolCode.USAGE_HISTORY_RESPONSE); // 재활용 가능
                    res.setData(restoHistory);
                    break;

                // ============================================================
                // 6. [기능 3] 기간별 결제 내역 조회 (관리자)
                // ============================================================
                case ProtocolCode.ADMIN_HISTORY_BY_PERIOD_REQUEST:
                    // 데이터 형식: "2023-01-01T00:00:00,2023-01-31T23:59:59" (String으로 가정)
                    String[] period = ((String) req.getData()).split(",");
                    LocalDateTime start = LocalDateTime.parse(period[0]);
                    LocalDateTime end = LocalDateTime.parse(period[1]);

                    List<PaymentDTO> periodHistory = paymentDAO.findHistoryByPeriod(start, end);

                    res.setCode(ProtocolCode.USAGE_HISTORY_RESPONSE);
                    res.setData(periodHistory);
                    break;

                // ============================================================
                // 7. [기능 4] 식당별 매출 현황 (관리자)
                // ============================================================
                case ProtocolCode.ADMIN_SALES_QUERY_REQUEST:
                    Map<String, Long> salesStats = paymentDAO.getSalesStatsByRestaurant();

                    res.setCode(ProtocolCode.ADMIN_SALES_QUERY_RESPONSE);
                    res.setData(salesStats); // Map 직렬화 전송
                    break;

                // ============================================================
                // 8. [기능 5] 시간대별 이용률 통계 (관리자)
                // ============================================================
                case ProtocolCode.ADMIN_TIME_STATS_REQUEST:
                    List<String> timeStats = paymentDAO.getTimeSlotUsageStats();

                    res.setCode(ProtocolCode.ADMIN_SALES_QUERY_RESPONSE); // 통계 응답 코드 재활용
                    res.setData(timeStats);
                    break;

                // ============================================================
                // 9. 쿠폰 구매
                // ============================================================
                case ProtocolCode.PURCHASE_COUPON_REQUEST:
                    List<CouponDTO> newCoupons = (List<CouponDTO>) req.getData();
                    boolean couponSuccess = couponDAO.insertCoupons(newCoupons);
                    res.setCode(couponSuccess ? ProtocolCode.SUCCESS : ProtocolCode.FAIL);
                    break;

                default:
                    System.out.println("알 수 없는 요청 코드: " + req.getCode());
                    res.setCode(ProtocolCode.FAIL);
                    break;
            }
        } catch (Exception e) {
            System.err.println("요청 처리 중 서버 오류: " + e.getMessage());
            e.printStackTrace();
            res.setType(ProtocolType.RESULT);
            res.setCode(ProtocolCode.SERVER_ERROR);
        }
        return res;
    }
}