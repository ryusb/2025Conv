package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

// Thread를 상속받아 클라이언트 연결 수락(accept) 기능을 전담합니다.
public class ConnectThread extends Thread {
    private final ServerSocket serverSocket;

    public ConnectThread(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    @Override
    public void run() {
        try {
            // 서버가 닫힐 때까지 무한히 연결을 기다립니다.
            while (!serverSocket.isClosed()) {
                // 1. 클라이언트 연결 수락
                Socket clientSocket = serverSocket.accept();
                System.out.println("🔗 새 클라이언트 접속: " + clientSocket.getInetAddress());

                // 2. ClientHandler 스레드에 요청 처리를 위임
                // ClientHandler는 이전에 구현한 (또는 구현할) 요청 처리 로직을 담고 있습니다.
                Thread clientHandler = new ClientHandler(clientSocket);
                clientHandler.start();
            }
        } catch (IOException e) {
            // ServerSocket.accept()가 종료될 때 발생하는 예외는 서버 종료로 간주합니다.
            // System.out.println("ConnectThread 종료: " + e.getMessage());
            // 무시해도 됩니다.
        }
    }
}