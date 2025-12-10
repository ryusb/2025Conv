package persistence.dao;

import persistence.dto.RestaurantDTO;
import network.DBConnectionManager;
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

                if (rs.getTime("open_time1") != null) {
                    resto.setOpenTime1(rs.getTime("open_time1").toLocalTime());
                }
                if (rs.getTime("close_time1") != null) {
                    resto.setCloseTime1(rs.getTime("close_time1").toLocalTime());
                }
                if (rs.getTime("open_time2") != null) {
                    resto.setOpenTime2(rs.getTime("open_time2").toLocalTime());
                }
                if (rs.getTime("close_time2") != null) {
                    resto.setCloseTime2(rs.getTime("close_time2").toLocalTime());
                }

                restaurantList.add(resto);
            }
        } catch (SQLException e) {
            System.err.println("RestaurantDAO - 식당 조회 중 DB 오류: " + e.getMessage());
        }
        return restaurantList;
    }

    public RestaurantDTO findById(int restaurantId) {
        String sql = "SELECT * FROM restaurant WHERE restaurant_id = ?";
        RestaurantDTO restaurant = null;

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, restaurantId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    restaurant = new RestaurantDTO();
                    restaurant.setRestaurantId(rs.getInt("restaurant_id"));
                    restaurant.setName(rs.getString("name"));

                    // DB TIME -> LocalTime 변환
                    // Time이 null일 경우를 대비해 체크할 수도 있으나, 여기선 필수값이라 가정
                    restaurant.setOpenTime1(rs.getTime("open_time1").toLocalTime());
                    restaurant.setCloseTime1(rs.getTime("close_time1").toLocalTime());
                    restaurant.setOpenTime2(rs.getTime("open_time2").toLocalTime());
                    restaurant.setCloseTime2(rs.getTime("close_time2").toLocalTime());
                }
            }
        } catch (SQLException e) {
            System.err.println("RestaurantDAO - 식당 조회 오류: " + e.getMessage());
        }
        return restaurant;
    }
}