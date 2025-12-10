package persistence.dao;

import persistence.dto.PaymentDTO;
import java.sql.*;
import network.DBConnectionManager;

public class PaymentDAO {

    // 결제 내역을 DB에 저장
    public boolean insertPayment(PaymentDTO payment) {
        String sql = "INSERT INTO payment (user_id, user_type, payment_time, restaurant_id, restaurant_name, " +
                "menu_price_id, menu_name, menu_price_at_time, used_coupon_id, coupon_value_used, " +
                "additional_card_amount, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, payment.getUserId());
            pstmt.setString(2, payment.getUserType());
            // LocalDateTime 처리 (MySQL DATETIME 변환)
            pstmt.setTimestamp(3, Timestamp.valueOf(payment.getPaymentTime()));
            pstmt.setInt(4, payment.getRestaurantId());
            pstmt.setString(5, payment.getRestaurantName());
            pstmt.setInt(6, payment.getMenuPriceId());
            pstmt.setString(7, payment.getMenuName());
            pstmt.setInt(8, payment.getMenuPriceAtTime());

            // used_coupon_id는 NULL 가능하므로 setObject/setNull 사용
            if (payment.getUsedCouponId() != null) {
                pstmt.setInt(9, payment.getUsedCouponId());
            } else {
                pstmt.setNull(9, Types.INTEGER);
            }

            pstmt.setInt(10, payment.getCouponValueUsed());
            pstmt.setInt(11, payment.getAdditionalCardAmount());
            pstmt.setString(12, payment.getStatus());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("PaymentDAO - 결제 삽입 중 DB 오류: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}