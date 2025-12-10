<<<<<<< HEAD
package persistence.dao;

import persistence.dto.MenuPriceDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import network.DBConnectionManager;

public class MenuPriceDAO {

    // 현재 학기, 특정 식당, 특정 시간대의 메뉴 목록을 조회
=======
package persistence.dao;

import persistence.dto.MenuPriceDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import network.DBConnectionManager;

public class MenuPriceDAO {

    // 현재 학기, 특정 식당, 특정 시간대의 메뉴 목록을 조회
>>>>>>> main
    public List<MenuPriceDTO> findCurrentMenus(int restaurantId, String mealTime) {
        List<MenuPriceDTO> menuList = new ArrayList<>();
        // is_current_semester=TRUE 필터링은 Java 코드 단순화를 위해 필수적입니다.
        String sql = "SELECT * FROM menu_price WHERE restaurant_id = ? AND meal_time = ? AND is_current_semester = TRUE";
<<<<<<< HEAD

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
                    // 나머지 필드 설정 생략

                    menuList.add(menu);
                }
            }
        } catch (SQLException e) {
            System.err.println("MenuPriceDAO - 메뉴 조회 중 DB 오류: " + e.getMessage());
=======

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
                    // 나머지 필드 설정 생략

                    menuList.add(menu);
                }
            }
        } catch (SQLException e) {
            System.err.println("MenuPriceDAO - 메뉴 조회 중 DB 오류: " + e.getMessage());
>>>>>>> main
        }
        return menuList;
    }
    // 메뉴 신규 등록
    public boolean insertMenu(MenuPriceDTO menu) {
        String sql = "INSERT INTO menu_price (restaurant_id, restaurant_name, semester_name, is_current_semester, meal_time, menu_name, price_stu, price_fac) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
<<<<<<< HEAD

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

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                return false;
            }

=======

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

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                return false;
            }

>>>>>>> main
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    menu.setMenuPriceId(generatedKeys.getInt(1));
                }
            }
            return true;
        } catch (SQLException e) {
<<<<<<< HEAD
            System.err.println("MenuPriceDAO - 메뉴 등록 중 DB 오류: " + e.getMessage());
=======
            System.err.println("RestaurantDAO - 메뉴 등록 중 DB 오류: " + e.getMessage());
>>>>>>> main
            return false;
        }
    }

    // 기존 메뉴 수정
<<<<<<< HEAD
    public boolean updateMenu(MenuPriceDTO menu) {
        String sql = "UPDATE menu_price SET restaurant_id = ?, restaurant_name = ?, semester_name = ?, is_current_semester = ?, " +
                "meal_time = ?, menu_name = ?, price_stu = ?, price_fac = ? WHERE menu_price_id = ?";

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, menu.getRestaurantId());
            pstmt.setString(2, menu.getRestaurantName());
            pstmt.setString(3, menu.getSemesterName());
            pstmt.setBoolean(4, menu.isCurrentSemester());
            pstmt.setString(5, menu.getMealTime());
            pstmt.setString(6, menu.getMenuName());
            pstmt.setInt(7, menu.getPriceStu());
            pstmt.setInt(8, menu.getPriceFac());
            pstmt.setInt(9, menu.getMenuPriceId());
=======
    public boolean updateMenu(MenuPriceDTO menu) {
        String sql = "UPDATE menu_price SET restaurant_id = ?, restaurant_name = ?, semester_name = ?, is_current_semester = ?, " +
                "meal_time = ?, menu_name = ?, price_stu = ?, price_fac = ? WHERE menu_price_id = ?";

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, menu.getRestaurantId());
            pstmt.setString(2, menu.getRestaurantName());
            pstmt.setString(3, menu.getSemesterName());
            pstmt.setBoolean(4, menu.isCurrentSemester());
            pstmt.setString(5, menu.getMealTime());
            pstmt.setString(6, menu.getMenuName());
            pstmt.setInt(7, menu.getPriceStu());
            pstmt.setInt(8, menu.getPriceFac());
            pstmt.setInt(9, menu.getMenuPriceId());
>>>>>>> main

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
<<<<<<< HEAD
            System.err.println("MenuPriceDAO - 메뉴 수정 중 DB 오류: " + e.getMessage());
=======
            System.err.println("RestaurantDAO - 메뉴 수정 중 DB 오류: " + e.getMessage());
>>>>>>> main
            return false;
        }
    }

    // 메뉴 id 존재 여부 확인
<<<<<<< HEAD
    public boolean existsById(int menuPriceId) {
        String sql = "SELECT 1 FROM menu_price WHERE menu_price_id = ?";
=======
    public boolean existsById(int menuPriceId) {
        String sql = "SELECT 1 FROM menu_price WHERE menu_price_id = ?";
>>>>>>> main
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, menuPriceId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
<<<<<<< HEAD
            System.err.println("MenuPriceDAO - 메뉴 존재 여부 확인 중 DB 오류: " + e.getMessage());
=======
            System.err.println("RestaurantDAO - 메뉴 존재 여부 확인 중 DB 오류: " + e.getMessage());
>>>>>>> main
            return false;
        }
    }

    // 메뉴 이미지 경로 업데이트
<<<<<<< HEAD
    public boolean updateMenuImagePath(int menuPriceId, String imagePath) {
        String sql = "UPDATE menu_price SET image_path = ? WHERE menu_price_id = ?";
=======
    public boolean updateMenuImagePath(int menuPriceId, String imagePath) {
        String sql = "UPDATE menu_price SET image_path = ? WHERE menu_price_id = ?";
>>>>>>> main
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, imagePath);
            pstmt.setInt(2, menuPriceId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
<<<<<<< HEAD
            System.err.println("MenuPriceDAO - 메뉴 이미지 경로 업데이트 중 DB 오류: " + e.getMessage());
=======
            System.err.println("RestaurantDAO - 메뉴 이미지 경로 업데이트 중 DB 오류: " + e.getMessage());
>>>>>>> main
            return false;
        }
    }

<<<<<<< HEAD
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

}
=======
}
>>>>>>> main
