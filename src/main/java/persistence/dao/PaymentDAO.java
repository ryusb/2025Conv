package persistence.dao;

import persistence.dto.PaymentDTO;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import network.DBConnectionManager;

public class PaymentDAO {

    // 결제 내역을 DB에 저장
    public boolean insertPayment(PaymentDTO payment) {
        String sql = "INSERT INTO payment (user_id, user_type, payment_time, restaurant_id, restaurant_name, " +
                "menu_price_id, menu_name, menu_price_at_time, used_coupon_id, coupon_value_used, " +
                "additional_card_amount, status) VALUES (?, ?, NOW(), ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, payment.getUserId());
            pstmt.setString(2, payment.getUserType());
            pstmt.setInt(3, payment.getRestaurantId());
            pstmt.setString(4, payment.getRestaurantName());
            pstmt.setInt(5, payment.getMenuPriceId());
            pstmt.setString(6, payment.getMenuName());
            pstmt.setInt(7, payment.getMenuPriceAtTime());
            if (payment.getUsedCouponId() != null) pstmt.setInt(8, payment.getUsedCouponId());
            else pstmt.setNull(8, Types.INTEGER);
            pstmt.setInt(9, payment.getCouponValueUsed());
            pstmt.setInt(10, payment.getAdditionalCardAmount());
            pstmt.setString(11, payment.getStatus());

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        payment.setPaymentId(rs.getInt(1)); // DTO에 ID 저장
                    }
                }
            }
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("PaymentDAO - 결제 삽입 중 DB 오류: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // 개인 결제 내역 조회 (사용자용)
    public List<PaymentDTO> findHistoryByUserId(int userId) {
        String sql = "SELECT * FROM payment WHERE user_id = ? ORDER BY payment_time DESC";
        return getPaymentList(sql, pstmt -> pstmt.setInt(1, userId));
    }

    // 식당별 결제 내역 조회 (관리자용)
    public List<PaymentDTO> findHistoryByRestaurantId(int restaurantId) {
        String sql = "SELECT * FROM payment WHERE restaurant_id = ? ORDER BY payment_time DESC";
        return getPaymentList(sql, pstmt -> pstmt.setInt(1, restaurantId));
    }

    // 기간별 결제 내역 조회 (관리자용)
    public List<PaymentDTO> findHistoryByPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        String sql = "SELECT * FROM payment WHERE payment_time BETWEEN ? AND ? ORDER BY payment_time DESC";
        return getPaymentList(sql, pstmt -> {
            pstmt.setTimestamp(1, Timestamp.valueOf(startDate));
            pstmt.setTimestamp(2, Timestamp.valueOf(endDate));
        });
    }

    // [공통] ResultSet -> PaymentDTO 변환 및 리스트 반환 헬퍼 메서드
    private List<PaymentDTO> getPaymentList(String sql, StatementSetter setter) {
        List<PaymentDTO> list = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (setter != null) setter.setValues(pstmt);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    PaymentDTO dto = new PaymentDTO();
                    dto.setPaymentId(rs.getInt("payment_id"));
                    dto.setUserId(rs.getInt("user_id"));
                    dto.setUserType(rs.getString("user_type"));
                    dto.setPaymentTime(rs.getTimestamp("payment_time").toLocalDateTime());
                    dto.setRestaurantId(rs.getInt("restaurant_id"));
                    dto.setRestaurantName(rs.getString("restaurant_name"));
                    dto.setMenuPriceId(rs.getInt("menu_price_id"));
                    dto.setMenuName(rs.getString("menu_name"));
                    dto.setMenuPriceAtTime(rs.getInt("menu_price_at_time"));

                    int cId = rs.getInt("used_coupon_id");
                    dto.setUsedCouponId(rs.wasNull() ? null : cId);

                    dto.setCouponValueUsed(rs.getInt("coupon_value_used"));
                    dto.setAdditionalCardAmount(rs.getInt("additional_card_amount"));
                    dto.setStatus(rs.getString("status"));
                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            System.err.println("PaymentDAO - 조회 오류: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    // 식당별 매출 현황 (총 매출액 계산)
    // 매출 = 쿠폰 사용액 + 추가 카드 결제액
    public Map<String, Long> getSalesStatsByRestaurant() {
        Map<String, Long> stats = new HashMap<>();
        String sql = "SELECT restaurant_name, SUM(coupon_value_used + additional_card_amount) as total_sales " +
                "FROM payment WHERE status = '성공' GROUP BY restaurant_name";

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                stats.put(rs.getString("restaurant_name"), rs.getLong("total_sales"));
            }
        } catch (SQLException e) {
            System.err.println("PaymentDAO - 매출 현황 오류: " + e.getMessage());
        }
        return stats;
    }

    // 시간대별 이용률 통계 (시간대별 결제 건수)
    // 반환 예시: "12시: 학생식당 (15건)", "12시: 교직원식당 (8건)"
    public List<String> getTimeSlotUsageStats() {
        List<String> result = new ArrayList<>();
        String sql = "SELECT HOUR(payment_time) as hour_slot, restaurant_name, COUNT(*) as usage_count "
                + "FROM payment WHERE status = '성공' "
                + "GROUP BY hour_slot, restaurant_name "
                + "ORDER BY hour_slot, restaurant_name";

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int hour = rs.getInt("hour_slot");
                String name = rs.getString("restaurant_name");
                int count = rs.getInt("usage_count");

                result.add(String.format("%02d시: %s (%d건)", hour, name, count));
            }
        } catch (SQLException e) {
            System.err.println("PaymentDAO - 시간대 통계 오류: " + e.getMessage());
        }
        return result;
    }

    // 람다식 사용을 위한 함수형 인터페이스
    @FunctionalInterface
    interface StatementSetter {
        void setValues(PreparedStatement pstmt) throws SQLException;
    }

    public LocalDateTime selectDbTime() {
        String sql = "SELECT NOW()";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getTimestamp(1).toLocalDateTime();
            }
        } catch (SQLException e) {
            System.err.println("PaymentDAO - 시간 조회 오류: " + e.getMessage());
        }
        return null;
    }
}