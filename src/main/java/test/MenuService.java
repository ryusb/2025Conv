package test;

import network.Protocol;
import network.ProtocolCode;
import network.ProtocolType;
import persistence.dto.MenuPriceDTO;
import util.InputHandler;
import util.OutputHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class MenuService {
    private static final Scanner sc = new Scanner(System.in);

    public static void viewMenuList(NetworkClient nc) throws IOException {
        int rid = InputHandler.getInt("식당 ID (1:학생, 2:교직원, 3:분식) : ");
        int time = InputHandler.getInt("시간대 (1,2) : ");

        MenuPriceDTO reqDto = new MenuPriceDTO();
        reqDto.setRestaurantId(rid);
        reqDto.setMealTime("opt"+time);

        nc.send(new Protocol(ProtocolType.REQUEST, ProtocolCode.MENU_LIST_REQUEST, reqDto));
        Protocol res = nc.receive();

        if (res.getCode() == ProtocolCode.MENU_LIST_RESPONSE) {
            List<MenuPriceDTO> list = (List<MenuPriceDTO>) res.getData();

            OutputHandler.showDoubleBar();
            OutputHandler.showMessage(" 📋 메뉴 목록 [" + list.size() + "개]\n");

            list.forEach(m -> System.out.printf("  [%2d] %-8s (학생:%,d원 / 직원:%,d원)\n",
                    m.getMenuPriceId(), m.getMenuName(), m.getPriceStu(), m.getPriceFac()));
        } else OutputHandler.showFail("실패 : "+res);
    }

    public static void downloadMenuImage(NetworkClient nc) throws IOException {
        int menuId = InputHandler.getInt("다운로드할 메뉴 ID: ");
        nc.send(new Protocol(ProtocolType.REQUEST, ProtocolCode.MENU_IMAGE_DOWNLOAD_REQUEST, menuId));
        Protocol res = nc.receive();

        if (res.getCode() == ProtocolCode.MENU_IMAGE_RESPONSE && res.getData() != null) {
            byte[] data = (byte[]) res.getData();
            String fileName = "menu_" + menuId + ".jpg";
            Files.write(Paths.get(fileName), data);
            OutputHandler.showSuccess("이미지 다운로드 완료 : " + fileName);
        } else {
            OutputHandler.showFail("이미지 다운로드 실패");
        }
    }

    // 관리자: 메뉴 관리
    public static void manageMenu(NetworkClient nc) throws IOException {
        while (true) {
            OutputHandler.showTitle("관리자 > 메뉴 관리");
            OutputHandler.showMenu(1,"메뉴 신규 등록");
            OutputHandler.showMenu(2,"메뉴 정보 수정 (이름/가격)");
            OutputHandler.showMenu(3,"메뉴 사진 등록");
            OutputHandler.showMenu(0,"뒤로가기");
            int choice = InputHandler.getInt("");
            if (choice == 0) return;
            switch (choice) {
                case 1 -> insertMenu(nc);
                case 2 -> updateMenu(nc);
                case 3 -> registerMenuImage(nc);
                default -> OutputHandler.showFail("잘못된 선택");
            }
        }
    }

    private static void insertMenu(NetworkClient nc) throws IOException {
        OutputHandler.showTitle("관리자 > 메뉴 관리 > 메뉴 등록");

        MenuPriceDTO m = new MenuPriceDTO();
        java.util.Scanner sc = new java.util.Scanner(System.in);

        m.setRestaurantId(InputHandler.getInt("식당 ID: "));
        m.setRestaurantName(InputHandler.getString("식당 이름: "));
        m.setMenuName(InputHandler.getString("메뉴명: "));
        m.setMealTime(InputHandler.getString("시간대  (1,2): "));
        m.setSemesterName(InputHandler.getString("학기명: "));
        m.setCurrentSemester(true);
        m.setPriceStu(InputHandler.getInt("학생가: "));
        m.setPriceFac(InputHandler.getInt("교직원가: "));
        String dateStr = InputHandler.getString("날짜 (YYYY-MM-DD): ");

        try {
            m.setDate(java.time.LocalDate.parse(dateStr).atStartOfDay());
        } catch (Exception e) {
            System.out.println("⚠️ 날짜 형식이 올바르지 않아 현재 날짜로 설정합니다.");
            m.setDate(java.time.LocalDateTime.now());
        }

        nc.send(new Protocol(ProtocolType.REQUEST, ProtocolCode.MENU_INSERT_REQUEST, m));
        Util.printSimpleResult(nc.receive());
    }

    private static void updateMenu(NetworkClient nc) throws IOException {
        System.out.println("\n[관리자: 메뉴 수정]");
        MenuPriceDTO m = new MenuPriceDTO();
        java.util.Scanner sc = new java.util.Scanner(System.in);

        System.out.print("수정할 메뉴 ID: "); m.setMenuPriceId(Util.getIntInput());
        System.out.print("새 메뉴명: "); m.setMenuName(sc.nextLine());
        System.out.print("새 학생가: "); m.setPriceStu(Util.getIntInput());
        System.out.print("새 교직원가: "); m.setPriceFac(Util.getIntInput());

        nc.send(new Protocol(ProtocolType.REQUEST, ProtocolCode.MENU_UPDATE_REQUEST, m));
        Util.printSimpleResult(nc.receive());
    }

    private static void registerMenuImage(NetworkClient nc) throws IOException {
        System.out.println("\n[관리자: 메뉴 사진 등록]");
        MenuPriceDTO m = new MenuPriceDTO();
        java.util.Scanner sc = new java.util.Scanner(System.in);

        System.out.print("메뉴 ID: "); m.setMenuPriceId(Util.getIntInput());
        System.out.print("업로드할 파일 경로: "); String path = sc.nextLine();

        try {
            byte[] data = Files.readAllBytes(Paths.get(path));
            m.setImageBytes(data);
            m.setUploadFileName(Paths.get(path).getFileName().toString());
            nc.send(new Protocol(ProtocolType.REQUEST, ProtocolCode.MENU_PHOTO_REGISTER_REQUEST, m));
            Util.printSimpleResult(nc.receive());
        } catch (Exception e) {
            System.out.println("❌ 파일 읽기 실패: " + e.getMessage());
        }
    }
}
