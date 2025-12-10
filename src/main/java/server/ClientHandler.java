package server;

import controller.MenuController;
import controller.CouponController;
import controller.PaymentController;
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Map;

import network.Protocol;
import network.ProtocolCode;
import network.ProtocolType;
import persistence.dao.PaymentDAO;
import persistence.dao.UserDAO;
import persistence.dto.CouponPolicyDTO;
import persistence.dto.MenuPriceDTO;
import persistence.dto.PaymentDTO;
import persistence.dto.UserDTO;

public class ClientHandler extends Thread {
    private final Socket clientSocket;

    // 컨트롤러 및 DAO 초기화
    private final MenuController menuController = new MenuController();
    private final CouponController couponController = new CouponController();
    private final PaymentController paymentController = new PaymentController();
    private final UserDAO userDAO = new UserDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();

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
        // 충분한 버퍼 크기 할당 (이미지 업로드 등 대비)
        byte[] buffer = new byte[1024 * 1024 * 2];
        int read = is.read(buffer);
        if (read == -1) throw new IOException("EOF");
        return java.util.Arrays.copyOf(buffer, read);
    }

    // 핵심 로직: 프로토콜 코드에 따른 분기 처리
    private Protocol handleRequest(Protocol req) {
        if (req.getType() != ProtocolType.REQUEST) {
            return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, null);
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
                        // 성공 시 LOGIN_RESPONSE (0x30) + 유저 데이터 반환
                        return new Protocol(ProtocolType.RESPONSE, ProtocolCode.LOGIN_RESPONSE, result);
                    } else {
                        // 실패 시 INVALID_INPUT (0x52) 반환
                        return new Protocol(ProtocolType.RESULT, ProtocolCode.INVALID_INPUT, null);
                    }
                }

                case ProtocolCode.MENU_LIST_REQUEST: { // 0x03
                    // 예: 1번 식당, 점심 메뉴 조회 (클라이언트 데이터에 따라 동적 처리 가능)
                    List<MenuPriceDTO> menus = menuController.getMenus(1, "점심");
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

                // ==========================================
                // II. 관리자 - 메뉴/가격 관리
                // ==========================================
                case ProtocolCode.MENU_INSERT_REQUEST:       // 0x10
                case ProtocolCode.MENU_UPDATE_REQUEST:       // 0x11
                    return menuController.registerOrUpdateMenu((MenuPriceDTO) req.getData());

                case ProtocolCode.MENU_PHOTO_REGISTER_REQUEST: // 0x12
                    return menuController.uploadMenuImage((MenuPriceDTO) req.getData());

                // ==========================================
                // III. 관리자 - 정책/보고서/CSV
                // ==========================================
                case ProtocolCode.COUPON_POLICY_INSERT_REQUEST: // 0x16
                    return couponController.upsertCouponPolicy((CouponPolicyDTO) req.getData());

                case ProtocolCode.ORDER_PAYMENT_HISTORY_REQUEST: { // 0x17
                    // 식당별 결제 내역 (클라이언트에서 식당ID 전송 가정)
                    int restaurantId = (int) req.getData();
                    List<PaymentDTO> list = paymentDAO.findHistoryByRestaurantId(restaurantId);
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
}