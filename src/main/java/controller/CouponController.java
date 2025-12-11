package controller;

import network.Protocol;
import network.ProtocolCode;
import network.ProtocolType;
import persistence.dao.CouponDAO;
import persistence.dao.CouponPolicyDAO;
import persistence.dto.CouponDTO;
import persistence.dto.CouponPolicyDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CouponController {

    private final CouponPolicyDAO couponPolicyDAO = new CouponPolicyDAO();
    private final CouponDAO couponDAO = new CouponDAO();

    // 쿠폰 구매 처리
    //현재 유효한 정책(가격)을 조회하여 그 가격으로 쿠폰을 생성합니다.
    public Protocol purchaseCoupons(int userId, int quantity) {
        if (quantity <= 0) {
            return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, null);
        }

        Optional<CouponPolicyDTO> policyOpt = couponPolicyDAO.findCurrentPolicy();
        int currentPrice = policyOpt.isPresent() ? policyOpt.get().getCouponPrice() : 5000;

        List<CouponDTO> newCoupons = new ArrayList<>();

        for (int i = 0; i < quantity; i++) {
            CouponDTO coupon = new CouponDTO();
            coupon.setUserId(userId);
            coupon.setPurchaseValue(currentPrice);
            coupon.setUsed(false);
            newCoupons.add(coupon);
        }

        boolean success = couponDAO.insertCoupons(newCoupons);
        return success
                ? new Protocol(ProtocolType.RESULT, ProtocolCode.SUCCESS, null)
                : new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, null);
    }

    // 사용자의 잔여 쿠폰 목록 조회
    public List<CouponDTO> getMyCoupons(int userId) {
        return couponDAO.findUnusedCouponsByUserId(userId);
    }

    /**
     * 쿠폰 정책(가격) 등록/수정: 새로운 정책 행을 추가하는 방식으로 처리.
     * effectiveDate 미지정 시 현재 시각 기준으로 저장한다.
     */
    public Protocol upsertCouponPolicy(CouponPolicyDTO policy) {
        if (!isValid(policy)) {
            return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, null);
        }

        boolean success = couponPolicyDAO.insertPolicy(policy);
        return success
                ? new Protocol(ProtocolType.RESULT, ProtocolCode.SUCCESS,  null)
                : new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, null);
    }

    public Protocol getCouponPolicies() {
        List<CouponPolicyDTO> list = couponPolicyDAO.findAllPolicies();

        // ProtocolCode.COUPON_POLICY_LIST_RESPONSE (0x35) 사용
        return new Protocol(ProtocolType.RESPONSE, ProtocolCode.COUPON_POLICY_LIST_RESPONSE, list);
    }

    private boolean isValid(CouponPolicyDTO policy) {
        if (policy == null) {
            return false;
        }
        return policy.getCouponPrice() > 0;
    }

    public Protocol getCouponPurchaseHistory(int userId) {
        List<CouponDTO> list = couponDAO.findAllCouponsByUserId(userId);
        return new Protocol(ProtocolType.RESPONSE, ProtocolCode.COUPON_PURCHASE_HISTORY_RESPONSE, list);
    }
}
