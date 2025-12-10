package persistence.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MenuPriceDTO implements DTO {
    private static final long serialVersionUID = 1L;

    private int menuPriceId;
    private int restaurantId;
    private String restaurantName;
    private String semesterName;
    private boolean isCurrentSemester;
    private String mealTime;    // '아침', '점심', '저녁', '상시'
    private String menuName;
    private String imagePath;   // 메뉴 이미지 서버 경로
    private LocalDateTime date;        //메뉴 날짜
    private int priceStu;       // 학생가
    private int priceFac;       // 직원가
    // 업로드용 추가 필드
    private byte[] imageBytes;    // 업로드되는 이미지 바이너리
    private String uploadFileName; // 클라이언트 원본 파일명 (확장자 확인용)

    public MenuPriceDTO() {}
}
