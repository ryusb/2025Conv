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

import java.time.LocalDateTime;

public class PaymentController {
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final MenuPriceDAO menuPriceDAO = new MenuPriceDAO();
    private final CouponDAO couponDAO = new CouponDAO();
    private final RestaurantDAO restaurantDAO = new RestaurantDAO();

    public Protocol processPayment(PaymentDTO request) {
        // 1. 메뉴 정보 확인 (현재 가격 확인)
        MenuPriceDTO menu = menuPriceDAO.findById(request.getMenuPriceId());
        if (menu == null) return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, null);

        // 1-1. 메뉴 날짜 유효성 체크 (DB 시간 기준)
        if (!menuPriceDAO.isMenuDateValid(request.getMenuPriceId())) {
            return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, "해당 메뉴는 오늘 제공되지 않습니다.");
        }

        // 1-2. 식당 영업 시간 체크 (DB 시간 기준)
        // restaurantDAO.isOpenNow()가 내부적으로 DB CURRENT_TIME() 사용
        if (!restaurantDAO.isOpenNow(menu.getRestaurantId())) {
            return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, "현재 식당 영업 시간이 아닙니다.");
        }

        // 2. 사용자 타입에 따른 가격 결정
        int currentPrice = request.getUserType().equals("교직원") ? menu.getPriceFac() : menu.getPriceStu();

        int couponValue = 0;
        int additionalCost = 0;
        String paymentStatus = "성공"; // 기본 상태

        // 3. 쿠폰 사용 여부 확인
        if (request.getUsedCouponId() != null && request.getUsedCouponId() > 0) {
            CouponDTO coupon = couponDAO.findById(request.getUsedCouponId());
            if (coupon == null || coupon.isUsed()) {
                return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, "유효하지 않은 쿠폰");
            }
            if (coupon.getUserId() != request.getUserId()) {
                return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, "본인의 쿠폰만 사용할 수 있습니다.");
            }
            couponValue = coupon.getPurchaseValue(); // 구매 당시 쿠폰 가치

            if (currentPrice > couponValue) {
                additionalCost = currentPrice - couponValue;
                paymentStatus = "추가금결제"; // 상태를 구체적으로 기록
            }

            // 우선 쿠폰 사용 처리
            couponDAO.updateCouponToUsed(coupon.getCouponId());
        } else {
            // 카드 결제 (전액)
            additionalCost = currentPrice;
            paymentStatus = "카드결제";
        }

        // 4. 추가금 결제 처리 (카드 결제 시뮬레이션)
        if (additionalCost > 0) {
            boolean cardSuccess = simulateCardPayment(request.getUserId(), additionalCost);
            if (!cardSuccess) {
                // 카드 결제 실패 시 쿠폰 사용 롤백 (Rollback)
                if (request.getUsedCouponId() != null && request.getUsedCouponId() > 0) {
                    couponDAO.updateCouponToUnused(request.getUsedCouponId());
                    System.out.println("[PaymentController] 카드 결제 실패로 쿠폰 사용을 취소했습니다. ID=" + request.getUsedCouponId());
                }
                return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, "카드 결제 승인 실패 (쿠폰 반환됨)");
            }
        }

        // 5. 결제 정보 완성 및 DB 저장
        request.setPaymentTime(LocalDateTime.now());
        request.setRestaurantId(menu.getRestaurantId());
        request.setRestaurantName(menu.getRestaurantName());
        request.setMenuName(menu.getMenuName());
        request.setMenuPriceAtTime(currentPrice);
        request.setCouponValueUsed(couponValue);
        request.setAdditionalCardAmount(additionalCost);
        request.setStatus(paymentStatus); // '성공', '추가금결제', '카드결제' 등

        boolean success = paymentDAO.insertPayment(request);

        if (success) {
            LocalDateTime dbTime = paymentDAO.selectDbTime();
            if (dbTime != null) {
                request.setPaymentTime(dbTime);
            }
            return new Protocol(ProtocolType.RESULT, ProtocolCode.SUCCESS, request);
        } else {
            if (request.getUsedCouponId() != null && request.getUsedCouponId() > 0) {
                couponDAO.updateCouponToUnused(request.getUsedCouponId());
            }
            return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, "결제 내역 저장 실패");
        }
    }

    // 카드 결제 시뮬레이션 (PG 연동 대용)
    private boolean simulateCardPayment(int userId, int amount) {
        System.out.println("[SERVER Log] 카드 결제 승인 요청: User=" + userId + ", Amount=" + amount + "원");
        return true; // 무조건 승인으로 가정
    }
}