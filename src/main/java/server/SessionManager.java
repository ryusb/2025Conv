package server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class SessionManager {
    // 로그인한 유저 ID와 해당 핸들러(연결)를 매핑하여 저장
    // ConcurrentHashMap을 사용하여 스레드 안전성 보장
    private static final Map<String, ClientHandler> activeSessions = new ConcurrentHashMap<>();

    // 로그인 시 호출: 이미 접속 중인지 확인하고 등록
    public static synchronized boolean addSession(String loginId, ClientHandler handler) {
        if (activeSessions.containsKey(loginId)) {
            return false; // 이미 접속 중임
        }
        activeSessions.put(loginId, handler);
        return true;
    }

    // 로그아웃 시 호출: 목록에서 제거
    public static synchronized void removeSession(String loginId) {
        if (loginId != null) {
            activeSessions.remove(loginId);
        }
    }

    // 접속 중인지 확인
    public static boolean isLoggedIn(String loginId) {
        return activeSessions.containsKey(loginId);
    }
}