package persistence.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MenuPriceDTO implements DTO {
    private static final long serialVersionUID = 1L;

    private int menuPriceId;
    private int restaurantId;
    private String restaurantName;
    private String semesterName;
    private boolean isCurrentSemester;
    private String mealTime; // '아침', '점심', '저녁', '상시'
    private String menuName;
    private String imagePath; // 메뉴 이미지 서버 경로
    private int priceStu;     // 학생가
    private int priceFac;     // 직원가

    public MenuPriceDTO() {}
}