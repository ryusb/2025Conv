package persistence.dao;

import persistence.dto.CouponDTO;
import network.DBConnectionManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CouponDAO {

    // 1. 사용자에게 새로운 쿠폰을 대량 삽입합니다. (쿠폰 구매)
    // List<CouponDTO>는 DB에 삽입할 쿠폰 정보를 담고 있습니다.
    public boolean insertCoupons(List<CouponDTO> coupons) {
        // user_id, purchase_date, purchase_value, is_used (false)
        String sql = "INSERT INTO coupon (user_id, purchase_date, purchase_value, is_used) VALUES (?, ?, ?, FALSE)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = true;

        try {
            conn = DBConnectionManager.getConnection();
            conn.setAutoCommit(false); // 트랜잭션 시작 (대량 삽입 시 효율적)
            pstmt = conn.prepareStatement(sql);

            for (CouponDTO coupon : coupons) {
                pstmt.setInt(1, coupon.getUserId());
                pstmt.setTimestamp(2, Timestamp.valueOf(coupon.getPurchaseDate()));
                pstmt.setInt(3, coupon.getPurchaseValue());
                pstmt.addBatch(); // 일괄 처리 목록에 추가
            }

            pstmt.executeBatch(); // 일괄 실행
            conn.commit(); // 커밋
        } catch (SQLException e) {
            success = false;
            try {
                if (conn != null) conn.rollback(); // 오류 시 롤백
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            System.err.println("CouponDAO - 쿠폰 대량 삽입 중 DB 오류: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (pstmt != null) pstmt.close();
                if (conn != null) {
                    conn.setAutoCommit(true); // AutoCommit 복구
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return success;
    }

    // 2. 특정 쿠폰을 사용 처리합니다.
    public boolean updateCouponToUsed(int couponId) {
        String sql = "UPDATE coupon SET is_used = TRUE WHERE coupon_id = ?";

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, couponId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("CouponDAO - 쿠폰 사용 처리 중 DB 오류: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public persistence.dto.CouponDTO findById(int couponId) {
        String sql = "SELECT * FROM coupon WHERE coupon_id = ?";
        persistence.dto.CouponDTO coupon = null;

        try (java.sql.Connection conn = network.DBConnectionManager.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, couponId);

            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    coupon = new persistence.dto.CouponDTO();
                    coupon.setCouponId(rs.getInt("coupon_id"));
                    coupon.setUserId(rs.getInt("user_id"));

                    // Timestamp -> LocalDateTime 변환
                    java.sql.Timestamp ts = rs.getTimestamp("purchase_date");
                    if (ts != null) coupon.setPurchaseDate(ts.toLocalDateTime());

                    coupon.setPurchaseValue(rs.getInt("purchase_value")); // 구매 당시 가치
                    coupon.setUsed(rs.getBoolean("is_used"));
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("CouponDAO - 쿠폰 조회 중 DB 오류: " + e.getMessage());
            e.printStackTrace();
        }
        return coupon;
    }
}