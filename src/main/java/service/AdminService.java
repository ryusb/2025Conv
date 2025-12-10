package service;

import util.InputHandler;
import util.OutputHandler;
import persistence.dao.MenuPriceDAO;
import persistence.dto.MenuPriceDTO;

public class AdminService {
    private static final MenuPriceDAO menuPriceDAO = new MenuPriceDAO();

    public static void mainService() {
        int choice;
        boolean isRunning = true;

        while (isRunning) {
            printMenu();
            choice = InputHandler.getInt("입력");

            switch (choice) {
                case 1 -> registerMenu();
                case 2 -> System.out.println("");
                case 3 -> System.out.println("");
                case 4 -> System.out.println("");
                case 5 -> System.out.println("");
                case 6 -> System.out.println("");
                case 7 -> isRunning = false;
                default -> OutputHandler.showError("잘못된 선택입니다");
            }
        }
    }

    private static void printMenu() {
        OutputHandler.showBar();
        OutputHandler.showTitle("관리자 메뉴");
        OutputHandler.showMenu(1, "메뉴 등록");
        OutputHandler.showMenu(2, "메뉴 수정(메뉴, 가격)");
        OutputHandler.showMenu(3, "메뉴 사진 등록");
        OutputHandler.showMenu(4, "쿠폰 관리");
        OutputHandler.showMenu(5, "현황 조회");
        OutputHandler.showMenu(6, "CSV 관리");
        OutputHandler.showMenu(7, "종료");
        OutputHandler.showBar();
    }

    /**
     * 메뉴 등록: 필수 필드 입력 후 DB에 저장.
     */
    private static void registerMenu() {
        OutputHandler.showTitle("메뉴 등록");
        OutputHandler.showMessage("식당 선택: 1) stdCafeteria  2) facCafeteria  3) snack");
        int restaurantChoice = InputHandler.getInt("선택(1~3)");
        int restaurantId = mapRestaurantId(restaurantChoice);
        String restaurantName = mapRestaurantName(restaurantChoice);
        if (restaurantId == -1 || restaurantName == null) {
            OutputHandler.showError("잘못된 식당 선택입니다.");
            return;
        }
        String semesterName = InputHandler.getString("학기명 (예: 2025-1학기)");
        char currentYN = InputHandler.getChar("현재 학기 적용? (Y/N)");
        boolean isCurrentSemester = (currentYN == 'Y');
        String mealTime = InputHandler.getString("식사 시간대 (아침/점심/저녁)");
        String menuName = InputHandler.getString("메뉴 이름");
        int priceStu = InputHandler.getInt("학생가");
        int priceFac = InputHandler.getInt("교직원가");

        MenuPriceDTO dto = new MenuPriceDTO();
        dto.setRestaurantId(restaurantId);
        dto.setRestaurantName(restaurantName);
        dto.setSemesterName(semesterName);
        dto.setCurrentSemester(isCurrentSemester);
        dto.setMealTime(mealTime);
        dto.setMenuName(menuName);
        dto.setPriceStu(priceStu);
        dto.setPriceFac(priceFac);

        boolean ok = menuPriceDAO.insertMenu(dto);
        if (ok) {
            OutputHandler.showSuccess("메뉴가 등록되었습니다. ID=" + dto.getMenuPriceId());
        } else {
            OutputHandler.showError("메뉴 등록에 실패했습니다.");
        }
    }

    // 식당 선택값을 ID로 매핑: 1->1(stdCafeteria), 2->2(facCafeteria), 3->3(snack)
    private static int mapRestaurantId(int choice) {
        return switch (choice) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            default -> -1;
        };
    }

    private static String mapRestaurantName(int choice) {
        return switch (choice) {
            case 1 -> "stdCafeteria";
            case 2 -> "facCafeteria";
            case 3 -> "snack";
            default -> null;
        };
    }
}
