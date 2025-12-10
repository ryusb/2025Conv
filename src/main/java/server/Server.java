package server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
// 불필요한 import 제거

public class Server {
    // ⚠️ IP는 서버 PC의 실제 IP 주소로 설정해야 합니다.
    private static final String IP = "192.168.45.75";
    private static final int PORT = 9000;

    // ServerSocket만 유지
    private ServerSocket serverSocket;

    public Server() {
        System.out.println("Server ready...");
    }

    public void run() {
        try {
            // IP 주소를 InetAddress 객체로 변환
            InetAddress ir = InetAddress.getByName(IP);

            // ServerSocket 생성 및 바인딩 (PORT, backlog=50, IP 주소)
            serverSocket = new ServerSocket(PORT, 50, ir);

            // 연결 수락을 전담할 스레드 시작
            ConnectThread connectThread = new ConnectThread(serverSocket);
            connectThread.start();

            System.out.println("서버 실행 완료. 종료를 위해 아무 숫자나 입력하세요.");

            // 메인 스레드는 종료 명령을 대기
            Scanner sc = new Scanner(System.in);
            sc.nextInt();

        } catch (IOException e) {
            System.err.println("서버 실행 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                    System.out.println("서버 소켓이 닫혔습니다. 서버 종료.");
                }
            } catch (Exception e) {
                System.out.println("서버 종료 오류: " + e);
            }
        }
    }

    public static void main(String[] args) {
        new Server().run();
    }
}