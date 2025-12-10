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
    private final PaymentController paymentController = new PaymentController();
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
            while (true) {
                // 1. 요청 수신
                byte[] receivedData;
                try {
                    receivedData = readProtocolFromClient(inputStream);
                } catch (IOException e) {
                    // 클라이언트 연결 종료 시 루프 탈출
                    System.out.println("클라이언트 연결 종료: " + clientSocket.getInetAddress());
                    break;
                }

                if (receivedData == null) {
                    break;
                }

                Protocol receivedProtocol = new Protocol(receivedData);
                System.out.println("수신된 요청 - 코드: 0x" + Integer.toHexString(receivedProtocol.getCode() & 0xFF).toUpperCase());

                // 2. 요청 처리
                Protocol response = handleRequest(receivedProtocol);

                // 3. 응답 전송
                outputStream.write(response.getBytes());
                outputStream.flush();
            }
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
                        return new Protocol(ProtocolType.RESPONSE, ProtocolCode.LOGIN_RESPONSE, 0, resultUser);                    } else {
                    }
                    return new Protocol(ProtocolType.RESULT, ProtocolCode.INVALID_INPUT, 0, null); // 또는 INVALID_INPUT
                }

                // --- 2. 메뉴 관련 ---
                case ProtocolCode.MENU_LIST_REQUEST: // 또는 MENU_LIST_REQUEST
                {
                    // 예시: 1번 식당, 점심 메뉴 조회
                    List<MenuPriceDTO> menus = menuController.getMenus(1, "점심");
                    return new Protocol(ProtocolType.RESPONSE, ProtocolCode.MENU_LIST_RESPONSE, 0, menus);
                }

                // --- 3. 결제 관련 ---
                case ProtocolCode.PAYMENT_CARD_REQUEST: // 카드 결제 요청 코드 사용 시
                {
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
                case ProtocolCode.MENU_INSERT_REQUEST: {
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
