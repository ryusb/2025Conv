package controller;

import network.Protocol;
import network.ProtocolCode;
import network.ProtocolType;
import persistence.dao.MenuPriceDAO;
import persistence.dto.MenuPriceDTO;

/**
 * 관리자 가격 등록/학기별 변경 컨트롤러.
 * - 분식당: 학기 선택 후 메뉴 선택 → 단일 메뉴 가격 등록/수정
 * - 학생/교직원 식당: 학기 선택 → 해당 식당 전체 메뉴 가격 일괄 변경
 */
public class PriceController {

    private final MenuPriceDAO menuPriceDAO = new MenuPriceDAO();

    /**
     * 단일 메뉴의 학기/가격 등록 또는 수정.
     * menuPriceId가 있으면 해당 메뉴 가격을 수정, 없으면 신규 학기 가격을 등록한다.
     */
    public Protocol upsertMenuPriceForSemester(MenuPriceDTO menu) {
        if (!isValidSingle(menu)) {
            return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, null);
        }

        boolean success;
        if (menu.getMenuPriceId() > 0) {
            if (!menuPriceDAO.existsById(menu.getMenuPriceId())) {
                return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, null);
            }
            success = menuPriceDAO.updateMenuPriceAndSemester(menu);
        } else {
            success = menuPriceDAO.insertMenu(menu);
        }

        return success
                ? new Protocol(ProtocolType.RESULT, ProtocolCode.SUCCESS, null)
                : new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, null);
    }

    /**
     * 특정 식당의 모든 메뉴에 대해 선택된 학기 가격을 일괄 변경.
     */
    public Protocol bulkUpdatePricesForSemester(MenuPriceDTO menu) {
        if (!isValidBulk(menu)) {
            return new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, null);
        }

        boolean success = menuPriceDAO.bulkUpdateSemesterPrices(
                menu.getRestaurantId(),
                menu.getSemesterName(),
                menu.isCurrentSemester(),
                menu.getPriceStu(),
                menu.getPriceFac()
        );

        return success
                ? new Protocol(ProtocolType.RESULT, ProtocolCode.SUCCESS, null)
                : new Protocol(ProtocolType.RESULT, ProtocolCode.FAIL, null);
    }

    private boolean isValidSingle(MenuPriceDTO menu) {
        if (menu == null) {
            return false;
        }
        if (menu.getRestaurantId() <= 0) {
            return false;
        }
        if (menu.getSemesterName() == null || menu.getSemesterName().trim().isEmpty()) {
            return false;
        }
        if (menu.getPriceStu() < 0 || menu.getPriceFac() < 0) {
            return false;
        }
        // 신규 등록 시 필요한 필드 체크
        if (menu.getMenuPriceId() <= 0) {
            if (menu.getRestaurantName() == null || menu.getRestaurantName().trim().isEmpty()) {
                return false;
            }
            if (menu.getMenuName() == null || menu.getMenuName().trim().isEmpty()) {
                return false;
            }
            if (menu.getMealTime() == null || menu.getMealTime().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidBulk(MenuPriceDTO menu) {
        if (menu == null) {
            return false;
        }
        if (menu.getRestaurantId() <= 0) {
            return false;
        }
        if (menu.getSemesterName() == null || menu.getSemesterName().trim().isEmpty()) {
            return false;
        }
        return menu.getPriceStu() >= 0 && menu.getPriceFac() >= 0;
    }
}
