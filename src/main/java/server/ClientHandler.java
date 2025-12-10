package server;

import controller.MenuController;
import controller.CouponController;
import controller.PaymentController;
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Map;

import network.Protocol; // Protocol 객체를 사용하여 통신 처리
import network.ProtocolCode;
import network.ProtocolType;
import persistence.dao.PaymentDAO;
import persistence.dao.UserDAO;
import persistence.dto.MenuPriceDTO;
import persistence.dto.PaymentDTO;
import persistence.dto.UserDTO;

public class ClientHandler extends Thread {
    private final Socket clientSocket;
    private final MenuController menuController = new MenuController();
    private final CouponController couponController = new CouponController();
    private final PaymentController paymentController = new PaymentController(); // 추가됨
    private final UserDAO userDAO = new UserDAO();

    // 생성자: 클라이언트 소켓을 받아서 초기화합니다.
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (
                InputStream inputStream = clientSocket.getInputStream();
                OutputStream outputStream = clientSocket.getOutputStream();
        ) {
            byte[] receivedData = readProtocolFromClient(inputStream);
            Protocol receivedProtocol = new Protocol(receivedData);

            System.out.println("수신된 요청 - 타입: " + receivedProtocol.getType() +
                    ", 코드: 0x" + Integer.toHexString(receivedProtocol.getCode() & 0xFF).toUpperCase());

            Protocol response = handleRequest(receivedProtocol);

            outputStream.write(response.getBytes());
            outputStream.flush();

        } catch (Exception e) {
            System.err.println("클라이언트 처리 중 오류 발생: " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    // ⚠️ TODO: InputStream에서 바이트 배열을 읽어오는 메서드 구현 필요
    private byte[] readProtocolFromClient(InputStream is) throws IOException {
        // 프로토콜의 전체 길이를 알 수 없으므로, 소켓 버퍼에서 데이터를 읽는 로직이 필요합니다.
        // 일반적으로 4바이트 헤더(전체 길이)를 먼저 읽고, 그 길이만큼 나머지 바디를 읽습니다.
        // 이 부분은 Protocol 구조에 맞게 구현되어야 합니다.
        // 임시로, 단순하게 4096 바이트만 읽는 코드로 대체합니다. (실제로는 수정 필요)
        byte[] buffer = new byte[4096 * 10];
        int bytesRead = is.read(buffer);
        if (bytesRead == -1) {
            throw new IOException("클라이언트 연결이 종료되었습니다.");
        }
        return java.util.Arrays.copyOf(buffer, bytesRead);
    }

    // 수신된 Protocol 객체를 분석하고 응답을 생성하는 메서드 구현
    private Protocol handleRequest(Protocol receivedProtocol) {
        if (receivedProtocol.getType() != ProtocolType.REQUEST) {
            return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, 0, null);
        }

        byte code = receivedProtocol.getCode();

        try {
            switch (code) {
                // --- 1. 사용자 인증 ---
                case ProtocolCode.LOGIN_REQUEST: {
                    UserDTO u = (UserDTO) receivedProtocol.getData();
                    UserDTO resultUser = userDAO.findUserByLoginId(u.getLoginId(), u.getPassword());
                    if (resultUser != null) {
                        return new Protocol(ProtocolType.RESPONSE, ProtocolCode.SUCCESS, 0, resultUser);
                    } else {
                        // INVALID_CREDENTIALS 대신 INVALID_INPUT 사용
                        return new Protocol(ProtocolType.RESULT, ProtocolCode.INVALID_INPUT, 0, null);
                    }
                }

                // --- 2. 메뉴 관련 ---
                case ProtocolCode.MENU_LIST_REQUEST: {
                    // 예: 전체 메뉴 혹은 오늘 메뉴 조회
                    // 클라이언트에서 조건을 보냈다면 받아서 처리 (여기선 예시로 특정 시간대 조회)
                    List<MenuPriceDTO> menus = menuController.getMenus(1, "점심");
                    return new Protocol(ProtocolType.RESPONSE, ProtocolCode.MENU_LIST_RESPONSE, 0, menus);
                }

                case ProtocolCode.MENU_IMAGE_DOWNLOAD_REQUEST: {
                    int menuId = (int) receivedProtocol.getData();
                    // 이미지 다운로드는 구현 필요 (MenuPriceDAO.findById 사용)
                    return new Protocol(ProtocolType.RESULT, ProtocolCode.NOT_FOUND, 0, null);
                }

                // --- 3. 결제 관련 ---
                case ProtocolCode.PAYMENT_CARD_REQUEST:
                case ProtocolCode.PAYMENT_COUPON_REQUEST: {
                    // 카드, 쿠폰 결제 모두 PaymentController가 처리 (DTO 안에 구분 정보 포함 가정)
                    PaymentDTO paymentReq = (PaymentDTO) receivedProtocol.getData();
                    return paymentController.processPayment(paymentReq);
                }

                case ProtocolCode.USAGE_HISTORY_REQUEST: {
                    int userId = (int) receivedProtocol.getData();
                    PaymentDAO paymentDAO = new PaymentDAO();
                    List<PaymentDTO> history = paymentDAO.findHistoryByUserId(userId);
                    return new Protocol(ProtocolType.RESPONSE, ProtocolCode.USAGE_HISTORY_RESPONSE, 0, history);
                }

                // --- 4. 관리자 기능 ---
                case ProtocolCode.MENU_INSERT_REQUEST:
                case ProtocolCode.MENU_UPDATE_REQUEST: {
                    // 등록과 수정을 하나의 컨트롤러 메서드에서 처리하거나 분기
                    return menuController.registerOrUpdateMenu((MenuPriceDTO) receivedProtocol.getData());
                }

                case ProtocolCode.MENU_PHOTO_REGISTER_REQUEST: {
                    return menuController.uploadMenuImage((MenuPriceDTO) receivedProtocol.getData());
                }

                case ProtocolCode.COUPON_POLICY_INSERT_REQUEST: {
                    return couponController.upsertCouponPolicy((persistence.dto.CouponPolicyDTO) receivedProtocol.getData());
                }

                case ProtocolCode.SALES_REPORT_REQUEST: {
                    PaymentDAO dao = new PaymentDAO();
                    Map<String, Long> stats = dao.getSalesStatsByRestaurant();
                    return new Protocol(ProtocolType.RESPONSE, ProtocolCode.SALES_REPORT_RESPONSE, 0, stats);
                }

                // [신규] CSV 파일 업로드
                case ProtocolCode.CSV_MENU_UPLOAD_REQUEST: {
                    byte[] csvBytes = (byte[]) receivedProtocol.getData(); // Byte 배열로 받는다고 가정
                    return menuController.registerMenuFromCSV(csvBytes);
                }

                default:
                    System.out.println("알 수 없는 코드: 0x" + Integer.toHexString(code));
                    return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, 0, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Protocol(ProtocolType.RESULT, ProtocolCode.SERVER_ERROR, 0, null);
        }
    }
}
