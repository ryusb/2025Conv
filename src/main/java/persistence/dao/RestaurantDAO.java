package persistence.dao;

import persistence.dto.RestaurantDTO;
import persistence.dto.MenuPriceDTO;
import network.DBConnectionManager; // ⚠️ 패키지 경로 변경
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RestaurantDAO {

    // 모든 식당 목록을 조회
    public List<RestaurantDTO> findAllRestaurants() {
        List<RestaurantDTO> restaurantList = new ArrayList<>();
        String sql = "SELECT * FROM restaurant";

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                RestaurantDTO resto = new RestaurantDTO();
                resto.setRestaurantId(rs.getInt("restaurant_id"));
                resto.setName(rs.getString("name"));

                // TIME 타입은 LocalTime으로 변환하여 DTO에 저장할 수 있으나,
                // 직렬화 단순화를 위해 String으로 유지할 수도 있습니다. (여기서는 String으로 가정)
                resto.setOpenTime(rs.getString("open_time"));
                resto.setCloseTime(rs.getString("close_time"));

                restaurantList.add(resto);
            }
        } catch (SQLException e) {
            System.err.println("RestaurantDAO - 식당 조회 중 DB 오류: " + e.getMessage());
        }
        return restaurantList;
    }

    // 식당 이름으로 식당 ID를 조회 (결제 처리에 필요)
    public int findRestaurantIdByName(String name) {
        String sql = "SELECT restaurant_id FROM restaurant WHERE name = ?";
        int id = -1;

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    id = rs.getInt("restaurant_id");
                }
            }
        } catch (SQLException e) {
            System.err.println("RestaurantDAO - ID 조회 중 DB 오류: " + e.getMessage());
        }
        return id;
    }
}