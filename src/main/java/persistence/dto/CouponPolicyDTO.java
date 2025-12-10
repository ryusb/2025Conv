package persistence.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class CouponPolicyDTO implements DTO {
    private static final long serialVersionUID = 1L;

    private int policyId;
    private int couponPrice;
    private LocalDateTime effectiveDate; // 정책 적용 시작일 (DB DATETIME)

    public CouponPolicyDTO() {}
}