package service;

import util.InputHandler;
import util.OutputHandler;
import persistence.dto.MenuPriceDTO;
import network.Protocol;
import network.ProtocolCode;
import network.ProtocolType;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminService {
    private static final String SERVER_IP = "118.216.49.188";
    private static final int PORT = 9000;

    public static void mainService() {
        int choice;
        boolean isRunning = true;

        while (isRunning) {
            printMenu();
            choice = InputHandler.getInt("입력");

            switch (choice) {
                case 1 -> registerMenu();
                case 2 -> updateMenu();
                case 3 -> System.out.println("");
                case 4 -> System.out.println("");
                case 5 -> System.out.println("");
                case 6 -> System.out.println("");
                case 7 -> isRunning = false;
                default -> OutputHandler.showFail("잘못된 선택입니다");
            }
        }
    }

    private static void printMenu() {
        OutputHandler.showSingleBar();
        OutputHandler.showTitle("관리자 메뉴");
        OutputHandler.showMenu(1, "메뉴 등록");
        OutputHandler.showMenu(2, "메뉴 수정(메뉴, 가격)");
        OutputHandler.showMenu(3, "메뉴 사진 등록");
        OutputHandler.showMenu(4, "쿠폰 관리");
        OutputHandler.showMenu(5, "현황 조회");
        OutputHandler.showMenu(6, "CSV 관리");
        OutputHandler.showMenu(7, "종료");
        OutputHandler.showSingleBar();
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
            OutputHandler.showFail("잘못된 식당 선택입니다.");
            return;
        }
        String semesterName = InputHandler.getString("학기명 (예: 2025-1)");
        char currentYN = InputHandler.getChar("현재 학기 적용? (Y/N)");
        boolean isCurrentSemester = (currentYN == 'Y');
        String mealTime = InputHandler.getString("식사 시간대 (아침/점심/저녁/상시)");
        String menuName = InputHandler.getString("메뉴 이름");
        String menuDate = InputHandler.getString("메뉴 날짜 (YYYY-MM-DD)");
        int priceStu = InputHandler.getInt("학생가");
        int priceFac = InputHandler.getInt("교직원가");

        MenuPriceDTO dto = new MenuPriceDTO();
        dto.setRestaurantId(restaurantId);
        dto.setRestaurantName(restaurantName);
        dto.setSemesterName(semesterName);
        dto.setCurrentSemester(isCurrentSemester);
        dto.setMealTime(mealTime);
        dto.setMenuName(menuName);
        dto.setMenuDate(menuDate);
        dto.setPriceStu(priceStu);
        dto.setPriceFac(priceFac);

        Protocol request = new Protocol(ProtocolType.REQUEST, ProtocolCode.MENU_INSERT_REQUEST, 0, dto);
        Protocol response = sendRequest(request);
        if (response != null && response.getCode() == ProtocolCode.SUCCESS) {
            OutputHandler.showSuccess("메뉴 등록 요청이 성공적으로 처리되었습니다.");
        } else {
            OutputHandler.showFail("메뉴 등록 요청 실패");
        }
    }

    /**
     * 메뉴 수정: 식당 선택 -> 날짜 입력 -> 해당 날짜 식당의 메뉴 리스트 출력 -> menu_price_id 선택 -> 새 메뉴명 입력 -> 서버에 수정 요청
     */
    private static void updateMenu() {
        OutputHandler.showTitle("메뉴 수정");
        OutputHandler.showMessage("식당 선택: 1) stdCafeteria  2) facCafeteria  3) snack");
        int restaurantChoice = InputHandler.getInt("선택(1~3)");
        int restaurantId = mapRestaurantId(restaurantChoice);
        String restaurantName = mapRestaurantName(restaurantChoice);
        if (restaurantId == -1 || restaurantName == null) {
            OutputHandler. showFail("잘못된 식당 선택입니다.");
            return;
        }

        String menuDate = InputHandler.getString("메뉴 날짜 (YYYY-MM-DD, 옵션)");
        // String mealTime = InputHandler.getString("식사 시간대 (아침/점심/저녁/상시)");

        // 1) 메뉴 목록 요청
        Map<String, Object> listReq = new HashMap<>();
        listReq.put("restaurantId", restaurantId);
        listReq.put("menuDate", menuDate);

        Protocol listRequest = new Protocol(ProtocolType.REQUEST, ProtocolCode.MENU_LIST_REQUEST, listReq);
        Protocol listResponse = sendRequest(listRequest);
        if (listResponse == null || listResponse.getCode() != ProtocolCode.MENU_LIST_RESPONSE) {
            OutputHandler. showFail("메뉴 목록을 불러오지 못했습니다.");
            return;
        }

        @SuppressWarnings("unchecked")
        List<MenuPriceDTO> menus = (List<MenuPriceDTO>) listResponse.getData();
        if (menus == null || menus.isEmpty()) {
            OutputHandler. showFail("해당 조건의 메뉴가 없습니다.");
            return;
        }

        OutputHandler.showMessage("수정 대상 선택 (menu_price_id: menu_name):");
        for (MenuPriceDTO m : menus) {
            System.out.println(m.getMenuPriceId() + " : " + m.getMenuName() + " (" + m.getPriceStu() + "원 / " + m.getPriceFac() + "원)");
        }

        // 2) 수정할 정보 입력 (메뉴명, 가격만 입력받음)
        int targetId = InputHandler.getInt("수정할 menu_price_id");
        String newMenuName = InputHandler.getString("새 메뉴 이름");
        int priceStu = InputHandler.getInt("학생가");
        int priceFac = InputHandler.getInt("교직원가");

        MenuPriceDTO dto = new MenuPriceDTO();
        dto.setMenuPriceId(targetId);
        dto.setMenuName(newMenuName);
        dto.setPriceStu(priceStu);
        dto.setPriceFac(priceFac);
        /*
        int targetId = InputHandler.getInt("수정할 menu_price_id");
        String newMenuName = InputHandler.getString("새 메뉴 이름");
        String semesterName = InputHandler.getString("학기명 (예: 2025-1)");
        char currentYN = InputHandler.getChar("현재 학기 적용? (Y/N)");
        boolean isCurrentSemester = (currentYN == 'Y');
        int priceStu = InputHandler.getInt("학생가");
        int priceFac = InputHandler.getInt("교직원가");

        MenuPriceDTO dto = new MenuPriceDTO();
        dto.setMenuPriceId(targetId);
        dto.setRestaurantId(restaurantId);
        dto.setRestaurantName(restaurantName);
        dto.setSemesterName(semesterName);
        dto.setCurrentSemester(isCurrentSemester);
        dto.setMealTime(mealTime);
        dto.setMenuName(newMenuName);
        dto.setMenuDate(menuDate);
        dto.setPriceStu(priceStu);
        dto.setPriceFac(priceFac);
        */
        Protocol updateRequest = new Protocol(ProtocolType.REQUEST, ProtocolCode.MENU_UPDATE_REQUEST, 0, dto);
        Protocol updateResponse = sendRequest(updateRequest);
        if (updateResponse != null && updateResponse.getCode() == ProtocolCode.SUCCESS) {
            OutputHandler.showSuccess("메뉴 수정 요청이 성공적으로 처리되었습니다.");
        } else {
            OutputHandler. showFail("메뉴 수정 요청 실패");
        }
    }

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

    // Client.java의 간단한 송수신 로직을 참고한 요청/응답 처리
    private static Protocol sendRequest(Protocol request) {
        try (
                Socket socket = new Socket(SERVER_IP, PORT);
                OutputStream os = socket.getOutputStream();
                InputStream is = socket.getInputStream()
        ) {
            os.write(request.getBytes());
            os.flush();

            byte[] header = new byte[6];
            int totalRead = 0;
            while (totalRead < 6) {
                int read = is.read(header, totalRead, 6 - totalRead);
                if (read == -1) break;
                totalRead += read;
            }
            if (totalRead < 6) {
                return null;
            }

            int dataLength = ((header[2] & 0xff) << 24) |
                    ((header[3] & 0xff) << 16) |
                    ((header[4] & 0xff) << 8) |
                    (header[5] & 0xff);

            byte[] body = new byte[dataLength];
            totalRead = 0;
            while (totalRead < dataLength) {
                int read = is.read(body, totalRead, dataLength - totalRead);
                if (read == -1) break;
                totalRead += read;
            }

            byte[] packet = new byte[6 + dataLength];
            System.arraycopy(header, 0, packet, 0, 6);
            System.arraycopy(body, 0, packet, 6, dataLength);
            return new Protocol(packet);
        } catch (Exception e) {
            OutputHandler. showFail("요청 송수신 중 오류: " + e.getMessage());
            return null;
        }
    }
}
