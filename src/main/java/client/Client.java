package client;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import network.*;
import persistence.dto.PaymentDTO;
import persistence.dto.UserDTO;
import service.AdminService;

public class Client {
    // ⚠️ TODO: 데스크톱의 실제 IP 주소를 여기에 입력하세요.
    private static final String SERVER_IP = "118.216.49.188";
    private static final int PORT = 9000;

    public static void main(String[] args) {
        // try-with-resources 구문: 여기서 socket, os, is가 생성되고, 블록이 끝나면 자동 종료됩니다.
        try (
                Socket socket = new Socket(SERVER_IP, PORT);
                OutputStream os = socket.getOutputStream();
                InputStream is = socket.getInputStream();
                Scanner sc = new Scanner(System.in)
        ) {
            System.out.println("🎉 서버 (" + SERVER_IP + ")에 성공적으로 접속했습니다.");

            while (true) {
                System.out.println("\n=== [테스트 메뉴] ===");
                System.out.println("1. 관리자 페이지 (로컬 실행)");
                System.out.println("2. 개인 이용 내역 조회 (로그인 가정)");
                System.out.println("3. 식당별 매출 현황 조회 (관리자)");
                System.out.println("4. 종료");
                System.out.print("선택> ");

                int choice = sc.nextInt();
                sc.nextLine(); // 버퍼 비우기

                if (choice == 4) break;

                Protocol request = null;

                switch (choice) {
                    case 1: // 관리자 페이지 로컬 실행
                        AdminService.mainService();
                        continue;

                    case 2: // 개인 이용 내역 조회
                        // 테스트를 위해 ID가 1인 유저라고 가정
                        int userId = 1;
                        request = new Protocol(ProtocolType.REQUEST, ProtocolCode.USAGE_HISTORY_REQUEST, 0, userId);
                        break;

                    case 3: // 식당별 매출 현황 조회
//                        request = new Protocol(ProtocolType.REQUEST, ProtocolCode.ADMIN_SALES_QUERY_REQUEST, 0, null);
                        break;

                    default:
                        System.out.println("잘못된 선택입니다.");
                        continue;
                }

                // 1. 요청 전송
                if (request != null) {
                    os.write(request.getBytes());
                    os.flush();
                    System.out.println("➡️ 요청 전송 완료.");
                }

                // 2. 응답 수신 (간단한 읽기 로직)
                // 실제로는 헤더를 먼저 읽고 길이를 파악해야 안전하지만, 테스트용으로 단순화함
                byte[] header = new byte[6];
                int totalRead = 0;
                while (totalRead < 6) {
                    int read = is.read(header, totalRead, 6 - totalRead);
                    if (read == -1) break;
                    totalRead += read;
                }

                if (totalRead == 6) {
                    int dataLength = ((header[2] & 0xff) << 24) |
                            ((header[3] & 0xff) << 16) |
                            ((header[4] & 0xff) << 8) |
                            (header[5] & 0xff);

                    byte[] body = new byte[dataLength];
                    totalRead = 0;
                    while (totalRead < dataLength) {
                        int read = is.read(body, totalRead, dataLength - totalRead);
                        if (read == -1) break;
                        totalRead += read;
                    }

                    byte[] packet = new byte[6 + dataLength];
                    System.arraycopy(header, 0, packet, 0, 6);
                    System.arraycopy(body, 0, packet, 6, dataLength);

                    Protocol response = new Protocol(packet);
                    System.out.println("⬅️ 응답 수신 완료. 코드: " + response.getCode());

                    Object data = response.getData();
                    if (response.getCode() == ProtocolCode.LOGIN_RESPONSE) {
                        UserDTO user = (UserDTO) data;
                        System.out.println("✅ 로그인 성공: " + user.getLoginId());
                    } else if (response.getCode() == ProtocolCode.USAGE_HISTORY_RESPONSE) {
                        List<PaymentDTO> list = (List<PaymentDTO>) data;
                        System.out.println("📄 내역 수: " + list.size());
                        for (PaymentDTO p : list) System.out.println(" - " + p.getMenuName());
                    }
//                    else if (response.getCode() == ProtocolCode.ADMIN_SALES_QUERY_RESPONSE) {
//                        Map<String, Long> sales = (Map<String, Long>) data;
//                        System.out.println("💰 매출: " + sales);
//                    }
                else {
                        System.out.println("❌ 실패 또는 알 수 없는 응답");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ 클라이언트 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
