package controller;

import network.Protocol;
import network.ProtocolCode;
import network.ProtocolType;
import persistence.dao.CouponDAO;
import persistence.dao.MenuPriceDAO;
import persistence.dao.PaymentDAO;
import persistence.dto.CouponDTO;
import persistence.dto.MenuPriceDTO;
import persistence.dto.PaymentDTO;

import java.time.LocalDateTime;

public class PaymentController {
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final MenuPriceDAO menuPriceDAO = new MenuPriceDAO();
    private final CouponDAO couponDAO = new CouponDAO();

    public Protocol processPayment(PaymentDTO request) {
        // 1. 메뉴 정보 확인 (현재 가격 확인)
        // MenuPriceDAO에 findById 메서드 추가 필요 혹은 기존 메서드 활용
        MenuPriceDTO menu = menuPriceDAO.findById(request.getMenuPriceId());
        if (menu == null) return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, null);

        // 2. 사용자 타입에 따른 가격 결정
        int currentPrice = request.getUserType().equals("교직원") ? menu.getPriceFac() : menu.getPriceStu();

        int payAmount = 0;
        int couponValue = 0;
        int additionalCost = 0;

        // 3. 쿠폰 사용 여부 확인
        if (request.getUsedCouponId() != null && request.getUsedCouponId() > 0) {
            CouponDTO coupon = couponDAO.findById(request.getUsedCouponId()); // CouponDAO에 구현 필요

            if (coupon == null || coupon.isUsed()) {
                // 유효하지 않은 쿠폰
                return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, "유효하지 않은 쿠폰");
            }

            couponValue = coupon.getPurchaseValue(); // 구매 당시 쿠폰 가치

            // 핵심 로직: 가격 인상으로 인한 차액 계산
            if (currentPrice > couponValue) {
                additionalCost = currentPrice - couponValue;
            }

            // 쿠폰 사용 처리
            couponDAO.updateCouponToUsed(coupon.getCouponId());
        } else {
            // 카드 결제 (전액)
            additionalCost = currentPrice;
        }

        // 4. 결제 정보 완성 및 DB 저장
        request.setPaymentTime(LocalDateTime.now());
        request.setMenuPriceAtTime(currentPrice);
        request.setCouponValueUsed(couponValue);
        request.setAdditionalCardAmount(additionalCost);
        request.setStatus("성공"); // 실제 결제 연동 없으므로 성공 처리

        boolean success = paymentDAO.insertPayment(request);

        if (success) {
            // 추가금이 발생했다면 클라이언트에게 알려줄 수도 있음 (여기서는 성공으로 리턴)
            return new Protocol(ProtocolType.RESULT, ProtocolCode.SUCCESS,  null);
        } else {
            return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL,  null);
        }
    }
}