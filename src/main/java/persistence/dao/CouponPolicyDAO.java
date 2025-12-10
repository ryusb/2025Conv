package persistence.dao;

import persistence.dto.CouponPolicyDTO;
import network.DBConnectionManager; // ⚠️ 패키지 경로 변경
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class CouponPolicyDAO {

    // 현재 시점에 유효한(가장 최근의) 쿠폰 정책을 조회
    public Optional<CouponPolicyDTO> findCurrentPolicy() {
        // effective_date가 가장 최근이면서, 현재 시각보다 이전인 정책을 찾습니다.
        String sql = "SELECT * FROM coupon_policy WHERE effective_date <= NOW() ORDER BY effective_date DESC LIMIT 1";

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                CouponPolicyDTO policy = new CouponPolicyDTO();
                policy.setPolicyId(rs.getInt("policy_id"));
                policy.setCouponPrice(rs.getInt("coupon_price"));
                // DB TIMESTAMP -> LocalDateTime 변환
                policy.setEffectiveDate(rs.getTimestamp("effective_date").toLocalDateTime());

                return Optional.of(policy);
            }
        } catch (SQLException e) {
            System.err.println("CouponPolicyDAO - 현재 정책 조회 중 DB 오류: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty(); // 정책이 없거나 오류 발생 시 빈 Optional 반환
    }

    // 새로운 쿠폰 정책을 등록 
    public boolean insertPolicy(CouponPolicyDTO policy) {
        String sql = "INSERT INTO coupon_policy (coupon_price, effective_date) VALUES (?, ?)";

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, policy.getCouponPrice());
            pstmt.setTimestamp(2, Timestamp.valueOf(policy.getEffectiveDate()));

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("CouponPolicyDAO - 정책 등록 중 DB 오류: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}