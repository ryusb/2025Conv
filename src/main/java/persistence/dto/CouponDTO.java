package persistence.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class CouponDTO implements DTO {
    private static final long serialVersionUID = 1L;

    private int couponId;
    private int userId;
    private LocalDateTime purchaseDate;
    private int purchaseValue; // 비정규화된 쿠폰 구매 시점 가치
    private boolean isUsed;

    public CouponDTO() {}
}