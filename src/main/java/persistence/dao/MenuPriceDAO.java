package persistence.dao;

import persistence.dto.MenuPriceDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import network.DBConnectionManager;

public class MenuPriceDAO {

    // 현재 학기, 특정 식당, 특정 시간대의 메뉴 목록을 조회
    public List<MenuPriceDTO> findCurrentMenus(int restaurantId, String mealTime) {
        List<MenuPriceDTO> menuList = new ArrayList<>();
        // CURDATE(): DB 서버의 현재 날짜 함수
        // date가 NULL인 경우(상시 메뉴)도 포함하거나, 날짜가 지정된 경우 오늘 날짜와 일치해야 함
        String sql = "SELECT * FROM menu_price WHERE restaurant_id = ? AND meal_time = ? " +
                "AND is_current_semester = TRUE " +
                "AND (date IS NULL OR DATE(date) = CURDATE())";

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, restaurantId);
            pstmt.setString(2, mealTime);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    MenuPriceDTO menu = new MenuPriceDTO();
                    menu.setMenuPriceId(rs.getInt("menu_price_id"));
                    menu.setRestaurantId(rs.getInt("restaurant_id"));
                    menu.setRestaurantName(rs.getString("restaurant_name"));
                    menu.setMenuName(rs.getString("menu_name"));
                    menu.setImagePath(rs.getString("image_path"));
                    menu.setPriceStu(rs.getInt("price_stu"));
                    menu.setPriceFac(rs.getInt("price_fac"));
                    if (rs.getTimestamp("date") != null) {
                        menu.setDate(rs.getTimestamp("date").toLocalDateTime());
                    }
                    menuList.add(menu);
                }
            }
        } catch (SQLException e) {
            System.err.println("MenuPriceDAO - 메뉴 조회 중 DB 오류: " + e.getMessage());
        }
        return menuList;
    }

    // 결제 요청 시 해당 메뉴가 '오늘(DB 기준)' 유효한지 검증
    public boolean isMenuDateValid(int menuPriceId) {
        String sql = "SELECT 1 FROM menu_price WHERE menu_price_id = ? " +
                "AND (date IS NULL OR DATE(date) = CURDATE())";

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, menuPriceId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // 결과가 있으면 유효한 날짜
            }
        } catch (SQLException e) {
            System.err.println("MenuPriceDAO - 메뉴 날짜 검증 오류: " + e.getMessage());
            return false;
        }
    }

    // 식당/날짜(옵션)/학기(옵션)/시간대(옵션)로 메뉴 목록 조회
    public List<MenuPriceDTO> findMenusByRestaurantAndDate(int restaurantId, String menuDate) {
        List<MenuPriceDTO> menuList = new ArrayList<>();
        // date 컬럼이 없을 수도 있으므로 try-catch로 감싼다. 없으면 날짜 조건을 생략한다.
        String sqlWithDate = "SELECT menu_price_id, menu_name FROM menu_price WHERE restaurant_id = ? AND date = ?";
        String sqlFallback = "SELECT menu_price_id, menu_name FROM menu_price WHERE restaurant_id = ?";
        try (Connection conn = DBConnectionManager.getConnection()) {
            PreparedStatement pstmt;
            if (menuDate != null && !menuDate.isBlank()) {
                try {
                    pstmt = conn.prepareStatement(sqlWithDate);
                    pstmt.setInt(1, restaurantId);
                    pstmt.setString(2, menuDate);
                } catch (SQLException syntax) {
                    // date 컬럼이 없는 경우 fallback
                    pstmt = conn.prepareStatement(sqlFallback);
                    pstmt.setInt(1, restaurantId);
                }
            } else {
                pstmt = conn.prepareStatement(sqlFallback);
                pstmt.setInt(1, restaurantId);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    MenuPriceDTO menu = new MenuPriceDTO();
                    menu.setMenuPriceId(rs.getInt("menu_price_id"));
                    menu.setMenuName(rs.getString("menu_name"));
                    menuList.add(menu);
                }
            }
        } catch (SQLException e) {
            System.err.println("MenuPriceDAO - 메뉴 목록 조회 중 DB 오류: " + e.getMessage());
        }
        return menuList;
    }
    // 메뉴 신규 등록
    public boolean insertMenu(MenuPriceDTO menu) {
        String sql = "INSERT INTO menu_price (restaurant_id, restaurant_name, semester_name, is_current_semester, meal_time, menu_name, price_stu, price_fac, date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, menu.getRestaurantId());
            pstmt.setString(2, menu.getRestaurantName());
            pstmt.setString(3, menu.getSemesterName());
            pstmt.setBoolean(4, menu.isCurrentSemester());
            pstmt.setString(5, menu.getMealTime());
            pstmt.setString(6, menu.getMenuName());
            pstmt.setInt(7, menu.getPriceStu());
            pstmt.setInt(8, menu.getPriceFac());

            if (menu.getDate() != null) {
                pstmt.setTimestamp(9, Timestamp.valueOf(menu.getDate()));
            } else {
                pstmt.setNull(9, Types.TIMESTAMP);
            }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) return false;

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    menu.setMenuPriceId(generatedKeys.getInt(1));
                }
            }
            return true;
        } catch (SQLException e) {
            System.err.println("MenuPriceDAO - 메뉴 등록 중 DB 오류: " + e.getMessage());
            return false;
        }
    }

    // 기존 메뉴 수정 (메뉴명, 가격만 수정)
    public boolean updateMenu(MenuPriceDTO menu) {
        String sql = "UPDATE menu_price SET menu_name = ?, price_stu = ?, price_fac = ? WHERE menu_price_id = ?";

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, menu.getMenuName());
            pstmt.setInt(2, menu.getPriceStu());
            pstmt.setInt(3, menu.getPriceFac());
            pstmt.setInt(4, menu.getMenuPriceId());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("MenuPriceDAO - 메뉴 수정 중 DB 오류: " + e.getMessage());
            return false;
        }
    }

    // 메뉴 id 존재 여부 확인
    public boolean existsById(int menuPriceId) {
        String sql = "SELECT 1 FROM menu_price WHERE menu_price_id = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, menuPriceId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("MenuPriceDAO - 메뉴 존재 여부 확인 중 DB 오류: " + e.getMessage());
            return false;
        }
    }

    // 메뉴 이미지 경로 업데이트
    public boolean updateMenuImagePath(int menuPriceId, byte[] imageBytes) {
        // 컬럼명은 image_path지만, 실제로는 BLOB 데이터를 저장합니다.
        String sql = "UPDATE menu_price SET image_path = ? WHERE menu_price_id = ?";

        try (java.sql.Connection conn = network.DBConnectionManager.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // setString 대신 setBytes 사용
            pstmt.setBytes(1, imageBytes);
            pstmt.setInt(2, menuPriceId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("MenuPriceDAO - 메뉴 이미지 경로 업데이트 중 DB 오류: " + e.getMessage());
            return false;
        }
    }

    // 단일 메뉴의 학기/가격 수정
    public boolean updateMenuPriceAndSemester(MenuPriceDTO menu) {
        String sql = "UPDATE menu_price SET semester_name = ?, is_current_semester = ?, price_stu = ?, price_fac = ? WHERE menu_price_id = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, menu.getSemesterName());
            pstmt.setBoolean(2, menu.isCurrentSemester());
            pstmt.setInt(3, menu.getPriceStu());
            pstmt.setInt(4, menu.getPriceFac());
            pstmt.setInt(5, menu.getMenuPriceId());
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("MenuPriceDAO - 메뉴 가격/학기 수정 중 DB 오류: " + e.getMessage());
            return false;
        }
    }

    // 특정 식당, 특정 학기에 대해 모든 메뉴 가격 일괄 수정
    public boolean bulkUpdateSemesterPrices(int restaurantId, String semesterName, boolean isCurrentSemester, int priceStu, int priceFac) {
        String sql = "UPDATE menu_price SET is_current_semester = ?, price_stu = ?, price_fac = ? WHERE restaurant_id = ? AND semester_name = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, isCurrentSemester);
            pstmt.setInt(2, priceStu);
            pstmt.setInt(3, priceFac);
            pstmt.setInt(4, restaurantId);
            pstmt.setString(5, semesterName);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("MenuPriceDAO - 식당 전체 메뉴 일괄 가격 수정 중 DB 오류: " + e.getMessage());
            return false;
        }
    }

    public MenuPriceDTO findById(int menuPriceId) {
        String sql = "SELECT * FROM menu_price WHERE menu_price_id = ?";
        MenuPriceDTO menu = null;

        try (java.sql.Connection conn = network.DBConnectionManager.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, menuPriceId);

            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    menu = new MenuPriceDTO();
                    menu.setMenuPriceId(rs.getInt("menu_price_id"));
                    menu.setRestaurantId(rs.getInt("restaurant_id"));
                    menu.setRestaurantName(rs.getString("restaurant_name"));
                    menu.setSemesterName(rs.getString("semester_name"));
                    menu.setCurrentSemester(rs.getBoolean("is_current_semester"));
                    menu.setMealTime(rs.getString("meal_time"));
                    menu.setMenuName(rs.getString("menu_name"));

                    // [중요] DB의 BLOB 데이터를 DTO의 byte[] 필드에 매핑
                    menu.setImageBytes(rs.getBytes("image_path"));

                    menu.setPriceStu(rs.getInt("price_stu"));
                    menu.setPriceFac(rs.getInt("price_fac"));
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("MenuPriceDAO - 단건 조회 중 DB 오류: " + e.getMessage());
            e.printStackTrace();
        }
        return menu;
    }
}
