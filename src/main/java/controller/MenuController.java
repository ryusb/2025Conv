package controller;

import network.Protocol;
import network.ProtocolCode;
import network.ProtocolType;
import persistence.dao.MenuPriceDAO;
import persistence.dao.RestaurantDAO;
import persistence.dto.MenuPriceDTO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 관리자 메뉴 등록/수정/ 이미지 저장 요청을 처리
 */
public class MenuController {

    private final MenuPriceDAO menupriceDAO = new MenuPriceDAO();

    /**
     * menuPriceId가 0이하면 신규 등록, 그 이상이면 수정으로 처리.
     */
    public Protocol registerOrUpdateMenu(MenuPriceDTO menu) {
        if (!isValid(menu)) {
            return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, 0, null);
        }

        boolean success;
        if (menu.getMenuPriceId() > 0) {
            if (!menupriceDAO.existsById(menu.getMenuPriceId())) {
                return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, 0, null);
            }
            success = menupriceDAO.updateMenu(menu);
        } else {
            success = menupriceDAO.insertMenu(menu);
        }

        return success
                ? new Protocol(ProtocolType.RESULT, ProtocolCode.SUCCESS, 0, null)
                : new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, 0, null);
    }

    /**
     * 메뉴 이미지 업로드: 파일을 서버에 저장하고 DB의 image_path를 갱신한다.
     */
    public Protocol uploadMenuImage(MenuPriceDTO menuImage) {
        // 유효성 검사
        if (menuImage == null || menuImage.getMenuPriceId() <= 0 ||
                menuImage.getImageBytes() == null || menuImage.getImageBytes().length == 0) {
            return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, 0, null);
        }

        // 메뉴 존재 여부 확인 (아래 2단계에서 구현할 findById 활용 가능)
        if (!menupriceDAO.existsById(menuImage.getMenuPriceId())) {
            return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, 0, null);
        }

        // 파일 시스템 저장 로직 제거 -> DAO 호출하여 DB에 바이트 저장
        boolean dbUpdated = menupriceDAO.updateMenuImagePath(menuImage.getMenuPriceId(), menuImage.getImageBytes());

        if (!dbUpdated) {
            return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, 0, null);
        }

        return new Protocol(ProtocolType.RESULT, ProtocolCode.SUCCESS, 0, null);
    }

    private boolean isValidForImage(MenuPriceDTO menuImage) {
        if (menuImage == null) {
            return false;
        }
        if (menuImage.getMenuPriceId() <= 0) {
            return false;
        }
        return menuImage.getImageBytes() != null && menuImage.getImageBytes().length > 0;
    }

    private String saveImage(MenuPriceDTO menuImage) throws IOException {
        String baseDir = "uploads/menu_images";
        Files.createDirectories(Path.of(baseDir));

        String original = menuImage.getUploadFileName() == null ? "" : menuImage.getUploadFileName();
        String ext = extractExtension(original);
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        String safeName = "menu_" + menuImage.getMenuPriceId() + "_" + timestamp + ext;

        Path target = Path.of(baseDir, safeName);
        Files.write(target, menuImage.getImageBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return target.toString();
    }

    private String extractExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx == -1 || idx == fileName.length() - 1) {
            return "";
        }
        String ext = fileName.substring(idx).toLowerCase();
        if (ext.contains("/") || ext.contains("\\")) {
            return "";
        }
        return ext;
    }

    private boolean isValid(MenuPriceDTO menu) {
        if (menu == null) {
            return false;
        }
        if (menu.getRestaurantId() <= 0) {
            return false;
        }
        if (menu.getMenuName() == null || menu.getMenuName().trim().isEmpty()) {
            return false;
        }
        if (menu.getMealTime() == null || menu.getMealTime().trim().isEmpty()) {
            return false;
        }
        if (menu.getSemesterName() == null || menu.getSemesterName().trim().isEmpty()) {
            return false;
        }
        if (menu.getPriceStu() < 0 || menu.getPriceFac() < 0) {
            return false;
        }
        return true;
    }

    public Protocol registerMenuFromCSV(byte[] csvFileBytes) {
        try {
            String content = new String(csvFileBytes, "UTF-8"); // 인코딩 주의 (EUC-KR or UTF-8)
            String[] lines = content.split("\n");

            int successCount = 0;

            // 첫 번째 줄이 헤더일 경우 i=1부터 시작
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;

                // CSV 포맷: 식당ID,식당명,학기,식사시간,메뉴명,학생가,직원가
                // 예: 1,학생식당,2025-1학기,점심,돈까스,5000,6000
                String[] tokens = line.split(",");

                if (tokens.length < 7) continue;

                MenuPriceDTO menu = new MenuPriceDTO();
                menu.setRestaurantId(Integer.parseInt(tokens[0].trim()));
                menu.setRestaurantName(tokens[1].trim());
                menu.setSemesterName(tokens[2].trim());
                menu.setMealTime(tokens[3].trim()); // 아침/점심/저녁
                menu.setMenuName(tokens[4].trim());
                menu.setPriceStu(Integer.parseInt(tokens[5].trim()));
                menu.setPriceFac(Integer.parseInt(tokens[6].trim()));
                menu.setCurrentSemester(true);

                if (menupriceDAO.insertMenu(menu)) {
                    successCount++;
                }
            }

            System.out.println("CSV 일괄 등록 완료: " + successCount + "건");
            return new Protocol(ProtocolType.RESULT, ProtocolCode.SUCCESS, 0, null);

        } catch (Exception e) {
            e.printStackTrace();
            return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, 0, null);
        }
    }
}
