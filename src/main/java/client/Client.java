package client;

import java.io.*;
import java.net.Socket;
import network.*;
import persistence.dto.UserDTO;

public class Client {
    // ⚠️ TODO: 데스크톱의 실제 IP 주소를 여기에 입력하세요.
    private static final String SERVER_IP = "118.216.49.188";
    private static final int PORT = 9000;

    public static void main(String[] args) {
        try (
                // 1. 서버에 접속 (Socket 생성)
                Socket socket = new Socket(SERVER_IP, PORT);
                OutputStream os = socket.getOutputStream();
                InputStream is = socket.getInputStream();
        ) {
            System.out.println("🎉 서버 (" + SERVER_IP + ")에 성공적으로 접속했습니다.");

            // 2. 테스트용 DTO 및 Protocol 생성 (예: 로그인 요청)
            UserDTO loginUser = new UserDTO();
            loginUser.setLoginId("testuser");
            loginUser.setPassword("1234");

            Protocol request = new Protocol(
                    ProtocolType.REQUEST,
                    ProtocolCode.LOGIN_REQUEST,
                    0, // DataLength는 getBytes()에서 자동 계산됨
                    loginUser
            );

            // 3. 요청 전송
            os.write(request.getBytes());
            os.flush();
            System.out.println("➡️ 로그인 요청 전송 완료.");

            // 4. 응답 수신
            // 서버 응답을 읽어오는 로직 (ClientHandler의 readProtocolFromClient와 유사)이 필요합니다.
            // 여기서는 단순화하여 4096 바이트만 읽는다고 가정합니다.
            byte[] responseData = new byte[4096];
            int bytesRead = is.read(responseData);
            if (bytesRead > 0) {
                byte[] actualData = java.util.Arrays.copyOf(responseData, bytesRead);
                Protocol response = new Protocol(actualData);

                System.out.println("⬅️ 응답 수신 완료. 코드: " + response.getCode());

                if (response.getCode() == ProtocolCode.LOGIN_RESPONSE) {
                    UserDTO loggedInUser = (UserDTO) response.getData();
                    System.out.println("✅ 로그인 성공! 사용자 이름: " + loggedInUser.getLoginId());
                } else if (response.getCode() == ProtocolCode.INVALID_CREDENTIALS) {
                    System.out.println("❌ 로그인 실패: ID 또는 비밀번호 오류");
                }
            }

        } catch (Exception e) {
            System.err.println("❌ 클라이언트 통신 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}