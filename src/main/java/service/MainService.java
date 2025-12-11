package service;

import client.NetworkClient;
import network.Protocol;
import network.ProtocolCode;
import persistence.dto.UserDTO;
import util.InputHandler;
import util.OutputHandler;

public class MainService {
    public static void run() {
        String role = login();

        if (role == null) { return; }

        switch (role) {
            case "admin" -> AdminService.mainService();
            case "other", "student" -> UserService.mainService();
            default -> OutputHandler.showFail("Invalid role");
        }
    }

    private static String login() {
        OutputHandler.showTitle("로그인");

        String id = InputHandler.getString("아이디");
        String pw = InputHandler.getString("비밀번호");

        UserDTO dto = new UserDTO();
        dto.setLoginId(id);
        dto.setPassword(pw);

        Protocol response = NetworkClient.sendRequest(
                ProtocolCode.LOGIN_REQUEST,
                dto
        );

        if (response == null) {
            OutputHandler.showFail("서버 응답 없음");
            return null;
        }

        if (response.getCode() == ProtocolCode.LOGIN_RESPONSE) {
            UserDTO user = (UserDTO) response.getData();
            OutputHandler.showSuccess(user.getLoginId() + " 로그인");
            return user.getUserType();   // "admin" / "student" / "other"
        }

        OutputHandler.showFail("로그인 실패");
        return null;
    }
}
