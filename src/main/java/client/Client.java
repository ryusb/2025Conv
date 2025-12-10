package client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

import network.*;
import persistence.dto.UserDTO;
import service.UserService;
import service.UserSession;

public class Client {

    private static final String SERVER_IP = "118.216.49.188";
    private static final int PORT = 9000;

    public static void main(String[] args) {
        try (
                Socket socket = new Socket(SERVER_IP, PORT);
                OutputStream os = socket.getOutputStream();
                InputStream is = socket.getInputStream();
                Scanner sc = new Scanner(System.in)
        ) {
            System.out.println("🎉 서버 (" + SERVER_IP + ")에 성공적으로 접속했습니다.");
            ClientSocketHolder.init(is, os);

            // ===========================
            // ✔ 1) 로그인만 처리
            // ===========================
            UserDTO loggedInUser = null;

            while (loggedInUser == null) {
                System.out.println("\n===== 로그인 =====");
                System.out.print("아이디: ");
                String loginId = sc.nextLine();

                System.out.print("패스워드: ");
                String password = sc.nextLine();

                UserDTO reqUser = new UserDTO();
                reqUser.setLoginId(loginId);
                reqUser.setPassword(password);

                Protocol request = new Protocol(
                        ProtocolType.REQUEST,
                        ProtocolCode.LOGIN_REQUEST,
                        reqUser
                );

                os.write(request.getBytes());
                os.flush();

                Protocol response = receive(is);

                if (response.getCode() == ProtocolCode.LOGIN_RESPONSE) {
                    loggedInUser = (UserDTO) response.getData();
                    System.out.println("✅ 로그인 성공: " + loggedInUser.getLoginId());

                    // 세션 저장
                    UserSession.setUser(loggedInUser);

                } else {
                    System.out.println("❌ 로그인 실패. 다시 시도하세요.");
                }
            }

            // ===========================
            // ✔ 2) 로그인 성공 → 사용자 메뉴 진입
            // ===========================
            UserService.mainService();

            System.out.println("클라이언트 종료.");

        } catch (Exception e) {
            System.err.println("❌ 클라이언트 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =======================
    //  패킷 수신 전용 메서드
    // =======================
    private static Protocol receive(InputStream is) throws IOException {
        byte[] header = new byte[6];

        int readBytes = 0;
        while (readBytes < 6) {
            int r = is.read(header, readBytes, 6 - readBytes);
            if (r == -1) throw new IOException("서버 연결 끊김");
            readBytes += r;
        }

        int dataLength =
                ((header[2] & 0xFF) << 24) |
                ((header[3] & 0xFF) << 16) |
                ((header[4] & 0xFF) << 8) |
                (header[5] & 0xFF);

        byte[] body = new byte[dataLength];
        readBytes = 0;
        while (readBytes < dataLength) {
            int r = is.read(body, readBytes, dataLength - readBytes);
            if (r == -1) throw new IOException("서버 연결 끊김");
            readBytes += r;
        }

        byte[] packet = new byte[6 + dataLength];
        System.arraycopy(header, 0, packet, 0, 6);
        System.arraycopy(body, 0, packet, 6, dataLength);

        return new Protocol(packet);
    }

}
