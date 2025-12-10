package persistence.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class PaymentDTO implements DTO {
    private static final long serialVersionUID = 1L;

    private int paymentId;
    private int userId;
    private String userType;
    private LocalDateTime paymentTime;
    private int restaurantId;
    private String restaurantName;
    private int menuPriceId;
    private String menuName;
    private int menuPriceAtTime;
    private Integer usedCouponId; // NULL 가능 -> Integer 사용
    private int couponValueUsed;
    private int additionalCardAmount; // 쿠폰 사용 후 추가 카드 결제 금액
    private String status; // '성공', '추가금납부', '실패'

    public PaymentDTO() {}
}