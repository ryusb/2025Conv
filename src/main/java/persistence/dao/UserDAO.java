package persistence.dao;

import persistence.dto.UserDTO;
import java.sql.*;
// DBConnectionManager는 앞서 정의된 가상의 DB 연결 관리 클래스라고 가정합니다.
import network.DBConnectionManager;

public class UserDAO {
    // 로그인 시 사용자 정보와 유형을 가져오는 핵심 메서드
    public UserDTO findUserByLoginId(String loginId, String password) {
        String sql = "SELECT * FROM user WHERE login_id = ? AND password = ?";
        UserDTO user = null;

        try (Connection conn = DBConnectionManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, loginId);
            // TODO: 해시화
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    user = new UserDTO();
                    user.setUserId(rs.getInt("user_id"));
                    user.setLoginId(rs.getString("login_id"));
                    user.setPassword(rs.getString("password"));
                    user.setUserType(rs.getString("user_type"));
                }
            }

        } catch (SQLException e) {
            System.err.println("UserDAO - 로그인 중 DB 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
        return user;
    }
}