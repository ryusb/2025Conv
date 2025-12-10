package network;

public class DBConfig {
    // ⚠️ DB 이름, 사용자 이름, 비밀번호를 실제 값으로 변경하세요.


    public static final String URL = "jdbc:mysql://localhost:3306/cafeteria?serverTimezone=UTC";

    public static final String USER = "root";
    public static final String PASSWORD = "kitproject";

    // 드라이버 이름 (MySQL 8.0 이상)
    public static final String DRIVER = "com.mysql.cj.jdbc.Driver";
}