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
        // is_current_semester=TRUE 필터링은 Java 코드 단순화를 위해 필수적입니다.
        String sql = "SELECT * FROM menu_price WHERE restaurant_id = ? AND meal_time = ? AND is_current_semester = TRUE";

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
        }
        return menuList;
    }
}