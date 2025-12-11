package server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class SessionManager {
    // 로그인한 유저 ID와 해당 핸들러(연결)를 매핑하여 저장
    private static final Map<String, ClientHandler> activeSessions = new ConcurrentHashMap<>();

    // 세션 등록 (이미 있으면 false 반환하지 않고, 호출하는 쪽에서 검사하도록 단순화)
    public static void addSession(String loginId, ClientHandler handler) {
        activeSessions.put(loginId, handler);
    }

    // 세션 제거
    public static void removeSession(String loginId) {
        if (loginId != null) {
            activeSessions.remove(loginId);
        }
    }

    // 현재 접속 중인 핸들러 조회 (중복 확인용)
    public static ClientHandler getSession(String loginId) {
        return activeSessions.get(loginId);
    }
}