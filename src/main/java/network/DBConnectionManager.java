package network;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnectionManager {

    /**
     * MySQL JDBC 드라이버를 로드하고 데이터베이스 연결을 설정합니다.
     * @return 유효한 Connection 객체
     * @throws SQLException 연결 실패 시 발생
     */
    public static Connection getConnection() throws SQLException {
        try {
            // 1. JDBC 드라이버 로드
            Class.forName(DBConfig.DRIVER);

            // 2. DB 연결 설정 및 Connection 객체 반환
            Connection conn = DriverManager.getConnection(
                    DBConfig.URL,
                    DBConfig.USER,
                    DBConfig.PASSWORD
            );
            return conn;

        } catch (ClassNotFoundException e) {
            System.err.println("❌ JDBC 드라이버를 찾을 수 없습니다: " + DBConfig.DRIVER);
            throw new SQLException("DB 드라이버 로드 실패", e);
        }
    }

    // 테스트용 main 메서드 (실제 서버에서는 필요 없음)
    public static void main(String[] args) {
        Connection conn = null;
        try {
            conn = getConnection();
            System.out.println("🎉 데이터베이스 연결 성공!");
        } catch (SQLException e) {
            System.err.println("❌ 데이터베이스 연결 실패: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                    System.out.println("🔗 연결 해제.");
                } catch (SQLException e) {
                    System.err.println("연결 해제 오류: " + e.getMessage());
                }
            }
        }
    }
}