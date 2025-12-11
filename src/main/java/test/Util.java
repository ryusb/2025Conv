package test;

import lombok.Getter;
import lombok.Setter;
import network.Protocol;
import network.ProtocolCode;
import network.ProtocolType;
import persistence.dto.UserDTO;
import util.InputHandler;
import util.OutputHandler;

import java.io.IOException;
import java.util.Scanner;

public class Util {
    private static final Scanner sc = new Scanner(System.in);
    @Getter
    @Setter
    private static int currentUserId;

    // -------------------- 공통 입력 --------------------
    public static int getIntInput() {
        try {
            return Integer.parseInt(sc.nextLine());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // -------------------- 로그인 --------------------
    public static int showLoginMenu() {
        OutputHandler.showTitle("시스템 접속");
        OutputHandler.showMenu(1, "로그인");
        OutputHandler.showMenu(0, "종료");
        return InputHandler.getInt("");
    }

    public static UserDTO loginProcess(NetworkClient nc) throws IOException {
        OutputHandler.showTitle("로그인");
        String id = InputHandler.getLogin(" ID : ");
        String pw = InputHandler.getLogin(" PW : ");

        UserDTO user = new UserDTO();
        user.setLoginId(id);
        user.setPassword(pw);

        nc.send(new Protocol(ProtocolType.REQUEST, ProtocolCode.LOGIN_REQUEST, user));
        Protocol res = nc.receive();

        if (res.getCode() == ProtocolCode.LOGIN_RESPONSE) {
            UserDTO loggedUser = (UserDTO) res.getData();
            OutputHandler.showSuccess("로그인 성공 : " + loggedUser.getUserType() + " " + loggedUser.getUserId() + "님 환영합니다");
            setCurrentUserId(loggedUser.getUserId());
            return loggedUser;
        } else {
            OutputHandler.showFail("로그인 실패: 아이디 또는 비밀번호를 확인하세요.");
            return null;
        }
    }

    // -------------------- 사용자 메뉴 --------------------
    public static int showUserMainMenu() {
        OutputHandler.showTitle("사용자");
        OutputHandler.showMenu(1, "주문 하기 (메뉴/결제)");
        OutputHandler.showMenu(2, "쿠폰 관리");
        OutputHandler.showMenu(3, "이용 내역 조회");
        OutputHandler.showMenu(0, "로그아웃");
        return InputHandler.getInt("");
    }

    public static int showUserOrderMenu() {
        OutputHandler.showTitle("사용자 > 주문");
        OutputHandler.showMenu(1, "메뉴 목록 조회");
        OutputHandler.showMenu(2, "메뉴 이미지 다운로드");
        OutputHandler.showMenu(3, "결제 하기");
        OutputHandler.showMenu(0, "뒤로가기");
        return InputHandler.getInt("");
    }

    public static int showUserPaymentMenu() {
        OutputHandler.showTitle("사용자 > 주문 > 결제");
        OutputHandler.showMenu(1, "카드 결제");
        OutputHandler.showMenu(2, "쿠폰 결제 (추가금 발생 가능)");
        OutputHandler.showMenu(0, "뒤로가기");
        return InputHandler.getInt("");
    }

    public static int showUserCouponMenu() {
        OutputHandler.showTitle("사용자 > 쿠폰");
        OutputHandler.showMenu(1, "내 쿠폰 조회");
        OutputHandler.showMenu(2, "쿠폰 구매");
        OutputHandler.showMenu(3, "쿠폰 구매 내역");
        OutputHandler.showMenu(0, "뒤로가기");
        return InputHandler.getInt("");
    }

    // -------------------- 관리자 메뉴 --------------------
    public static int showAdminMainMenu() {
        OutputHandler.showTitle("관리자");
        OutputHandler.showMenu(1, "메뉴 관리 (등록/수정/사진))");
        OutputHandler.showMenu(2, "가격 책정 (분식/일반)");
        OutputHandler.showMenu(3, "쿠폰 정책 관리");
        OutputHandler.showMenu(4, "통계 및 보고서");
        OutputHandler.showMenu(5, "데이터 관리 (CSV)");
        OutputHandler.showMenu(0, "로그아웃");
        return InputHandler.getInt("");
    }

    // -------------------- 결과 출력 --------------------
    public static void printSimpleResult(Protocol res) {
        if (res.getCode() == ProtocolCode.SUCCESS) OutputHandler.showSuccess("쿠폰 구매 성공");
        else if (res.getCode() == ProtocolCode.FAIL) OutputHandler.showFail("쿠폰 구매 실패 : " + res.getData());
        else if (res.getCode() == ProtocolCode.PERMISSION_DENIED) OutputHandler.showFail("권한 없음");
        else OutputHandler.showOut("기타 응답 코드: 0x" + Integer.toHexString(res.getCode()));
    }

    public static void printFail(Protocol res) {
        OutputHandler.showFail(" 실패 : " + res.getData());
    }
}
