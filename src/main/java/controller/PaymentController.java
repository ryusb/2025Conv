package controller;

import network.Protocol;
import network.ProtocolCode;
import network.ProtocolType;
import persistence.dao.CouponDAO;
import persistence.dao.MenuPriceDAO;
import persistence.dao.PaymentDAO;
import persistence.dao.RestaurantDAO;
import persistence.dto.CouponDTO;
import persistence.dto.MenuPriceDTO;
import persistence.dto.PaymentDTO;
import persistence.dto.RestaurantDTO;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class PaymentController {
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final MenuPriceDAO menuPriceDAO = new MenuPriceDAO();
    private final CouponDAO couponDAO = new CouponDAO();
    private final RestaurantDAO restaurantDAO = new RestaurantDAO();

    public Protocol processPayment(PaymentDTO request) {
        // 1. 메뉴 정보 확인 (현재 가격 확인)
        MenuPriceDTO menu = menuPriceDAO.findById(request.getMenuPriceId());
        if (menu == null) return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, null);

        // ============================================================
        // [추가] 1-1. 식당 영업 시간 체크 로직
        // ============================================================
        RestaurantDTO restaurant = restaurantDAO.findById(menu.getRestaurantId());
        if (restaurant != null) {
            // 현재 시간이 식당 영업 시간(1타임 또는 2타임)에 포함되는지 확인
            if (!isRestaurantOpen(restaurant)) {
                // 영업 시간이 아니라면 결제 거절 (메시지 포함)
                return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, "현재 식당 영업 시간이 아닙니다.");
            }
        }
        // ============================================================

        // 2. 사용자 타입에 따른 가격 결정
        int currentPrice = request.getUserType().equals("교직원") ? menu.getPriceFac() : menu.getPriceStu();

        int payAmount = 0;
        int couponValue = 0;
        int additionalCost = 0;

        // 3. 쿠폰 사용 여부 확인
        if (request.getUsedCouponId() != null && request.getUsedCouponId() > 0) {
            CouponDTO coupon = couponDAO.findById(request.getUsedCouponId());

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
        request.setRestaurantId(menu.getRestaurantId());
        request.setRestaurantName(menu.getRestaurantName());
        request.setMenuName(menu.getMenuName());
        request.setMenuPriceAtTime(currentPrice);
        request.setCouponValueUsed(couponValue);
        request.setAdditionalCardAmount(additionalCost);
        request.setStatus("성공"); // 실제 결제 연동 없으므로 성공 처리

        boolean success = paymentDAO.insertPayment(request);

        if (success) {
            // 성공 시, 추가금 등의 메시지를 보낼 수도 있지만 여기선 성공 코드만 리턴
            return new Protocol(ProtocolType.RESULT, ProtocolCode.SUCCESS, null);
        } else {
            return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, null);
        }
    }

    private boolean isRestaurantOpen(RestaurantDTO r) {
        LocalTime now = LocalTime.now();

        // 1타임(예: 아침/점심) 운영 여부 확인
        // null 체크: DB에 시간이 비어있지 않다고 가정하지만 안전을 위해 추가 가능
        boolean isOpen1 = r.getOpenTime1() != null && r.getCloseTime1() != null &&
                !now.isBefore(r.getOpenTime1()) && !now.isAfter(r.getCloseTime1());

        // 2타임(예: 저녁) 운영 여부 확인
        boolean isOpen2 = r.getOpenTime2() != null && r.getCloseTime2() != null &&
                !now.isBefore(r.getOpenTime2()) && !now.isAfter(r.getCloseTime2());

        // 둘 중 하나라도 열려있으면 영업 중(true)
        return isOpen1 || isOpen2;
    }
}