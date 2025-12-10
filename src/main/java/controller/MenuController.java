package controller;

import network.Protocol;
import network.ProtocolCode;
import network.ProtocolType;
import persistence.dao.MenuPriceDAO;
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
        if (!isValidForImage(menuImage)) {
            return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, 0, null);
        }

        if (!menupriceDAO.existsById(menuImage.getMenuPriceId())) {
            return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, 0, null);
        }

        String storedPath;
        try {
            storedPath = saveImage(menuImage);
        } catch (IOException e) {
            System.err.println("MenuController - 이미지 저장 실패: " + e.getMessage());
            return new Protocol(ProtocolType.RESULT, ProtocolCode.SERVER_ERROR, 0, null);
        }

        boolean dbUpdated =menupriceDAO.updateMenuImagePath(menuImage.getMenuPriceId(), storedPath);
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
}
