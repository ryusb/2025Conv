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

    /**
     * [추가됨] 쿠폰 구매 처리
     * 현재 유효한 정책(가격)을 조회하여 그 가격으로 쿠폰을 생성합니다.
     */
    public Protocol purchaseCoupons(int userId, int quantity) {
        if (quantity <= 0) {
            return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, null);
        }

        // 1. 현재 적용 가능한 쿠폰 정책 조회
        Optional<CouponPolicyDTO> policyOpt = couponPolicyDAO.findCurrentPolicy();
        int currentPrice;

        if (policyOpt.isPresent()) {
            currentPrice = policyOpt.get().getCouponPrice();
        } else {
            // 정책이 없으면 기본값 설정 혹은 에러 처리 (여기선 예시로 5000원)
            currentPrice = 5000;
        }

        // 2. 쿠폰 생성 (메모리 상)
        List<CouponDTO> newCoupons = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < quantity; i++) {
            CouponDTO coupon = new CouponDTO();
            coupon.setUserId(userId);
            coupon.setPurchaseDate(now);
            coupon.setPurchaseValue(currentPrice); // [중요] 시점의 가격 저장
            coupon.setUsed(false);
            newCoupons.add(coupon);
        }

        // 3. DB 저장
        boolean success = couponDAO.insertCoupons(newCoupons);

        return success
                ? new Protocol(ProtocolType.RESULT, ProtocolCode.SUCCESS, null)
                : new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, null);
    }

    /**
     * [추가됨] 사용자의 잔여 쿠폰 목록 조회
     */
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

        if (policy.getEffectiveDate() == null) {
            policy.setEffectiveDate(LocalDateTime.now());
        }

        boolean success = couponPolicyDAO.insertPolicy(policy);
        return success
                ? new Protocol(ProtocolType.RESULT, ProtocolCode.SUCCESS,  null)
                : new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, null);
    }

    private boolean isValid(CouponPolicyDTO policy) {
        if (policy == null) {
            return false;
        }
        return policy.getCouponPrice() > 0;
    }
}
