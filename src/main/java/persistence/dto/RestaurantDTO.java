package persistence.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class RestaurantDTO implements DTO {
    private static final long serialVersionUID = 1L;

    private int restaurantId;
    private String name;
    private LocalTime openTime1;
    private LocalTime closeTime1;
    private LocalTime openTime2;
    private LocalTime closeTime2;

    public RestaurantDTO() {}
}