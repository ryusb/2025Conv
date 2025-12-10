package server;

import controller.MenuController;
import controller.CouponController;
import java.io.*;
import java.net.Socket;
import network.Protocol; // Protocol 객체를 사용하여 통신 처리
import network.ProtocolCode;
import network.ProtocolType;
import persistence.dto.MenuPriceDTO;

public class ClientHandler extends Thread {
    private final Socket clientSocket;
    private final MenuController menuController = new MenuController();
    private final CouponController couponController = new CouponController();

    // 생성자: 클라이언트 소켓을 받아서 초기화합니다.
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
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

    // 수신된 Protocol 객체를 분석하고 응답을 생성하는 메서드 구현
    private Protocol handleRequest(Protocol receivedProtocol) {
        if (receivedProtocol.getType() != ProtocolType.REQUEST) {
            return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, 0, null);
        }

        byte code = receivedProtocol.getCode();
        switch (code) {
            case ProtocolCode.ADMIN_MENU_REGISTER_REQUEST: {
                Object data = receivedProtocol.getData();
                if (!(data instanceof MenuPriceDTO)) {
                    return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, 0, null);
                }
                return menuController.registerOrUpdateMenu((MenuPriceDTO) data);
            }
            case ProtocolCode.ADMIN_IMAGE_UPLOAD_REQUEST: {
                Object data = receivedProtocol.getData();
                if (!(data instanceof MenuPriceDTO)) {
                    return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, 0, null);
                }
                return menuController.uploadMenuImage((MenuPriceDTO) data);
            }
            case ProtocolCode.ADMIN_POLICY_REGISTER_REQUEST: {
                Object data = receivedProtocol.getData();
                if (!(data instanceof persistence.dto.CouponPolicyDTO)) {
                    return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, 0, null);
                }
                return couponController.upsertCouponPolicy((persistence.dto.CouponPolicyDTO) data);
            }
            default:
                return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, 0, null);
        }
    }
}
