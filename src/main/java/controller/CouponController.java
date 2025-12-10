package controller;

import network.Protocol;
import network.ProtocolCode;
import network.ProtocolType;
import persistence.dao.CouponPolicyDAO;
import persistence.dto.CouponPolicyDTO;

import java.time.LocalDateTime;

public class CouponController {

    private final CouponPolicyDAO couponPolicyDAO = new CouponPolicyDAO();

    /**
     * 쿠폰 정책(가격) 등록/수정: 새로운 정책 행을 추가하는 방식으로 처리.
     * effectiveDate 미지정 시 현재 시각 기준으로 저장한다.
     */
    public Protocol upsertCouponPolicy(CouponPolicyDTO policy) {
        if (!isValid(policy)) {
            return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, 0, null);
        }

        if (policy.getEffectiveDate() == null) {
            policy.setEffectiveDate(LocalDateTime.now());
        }

        boolean success = couponPolicyDAO.insertPolicy(policy);
        return success
                ? new Protocol(ProtocolType.RESULT, ProtocolCode.SUCCESS, 0, null)
                : new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, 0, null);
    }

    private boolean isValid(CouponPolicyDTO policy) {
        if (policy == null) {
            return false;
        }
        return policy.getCouponPrice() > 0;
    }
}
