package persistence.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RestaurantDTO implements DTO {
    private static final long serialVersionUID = 1L;

    private int restaurantId;
    private String name;
    private String openTime;  // DB TIME -> String 또는 LocalTime
    private String closeTime; // DB TIME -> String 또는 LocalTime

    public RestaurantDTO() {}
}