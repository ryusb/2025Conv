package network;

public class ProtocolCode {
    // -----------------------------------------------------------
    // Type: REQUEST (클라이언트 -> 서버) - 0x01 ~ 0x21 범위
    // -----------------------------------------------------------

    // I. 사용자 인증 및 결제 (0x01 ~ 0x09)
    public static final byte SIGNUP_REQUEST = 0x01;              // 회원가입 요청
    public static final byte LOGIN_REQUEST = 0x02;               // 로그인 요청
    public static final byte MENU_LIST_REQUEST = 0x03;           // 메뉴 목록 조회 요청
    public static final byte MENU_IMAGE_DOWNLOAD_REQUEST = 0x04; // 메뉴 이미지 다운로드 요청
    public static final byte COUPON_LIST_REQUEST = 0x05;         // 쿠폰 조회 요청
    public static final byte COUPON_PURCHASE_REQUEST = 0x06;     // 쿠폰 구매 요청
    public static final byte PAYMENT_CARD_REQUEST = 0x07;        // 카드 결제 요청
    public static final byte PAYMENT_COUPON_REQUEST = 0x08;      // 쿠폰 결제 요청
    public static final byte USAGE_HISTORY_REQUEST = 0x09;       // 이용 내역 조회 요청

    // II. 관리자 - 메뉴/가격 관리 (0x10 ~ 0x14)
    public static final byte MENU_INSERT_REQUEST = 0x10;         // 메뉴 등록 요청
    public static final byte MENU_UPDATE_REQUEST = 0x11;         // 메뉴 수정 요청
    public static final byte MENU_PHOTO_REGISTER_REQUEST = 0x12; // 메뉴 사진 등록 요청
    public static final byte PRICE_REGISTER_SNACK_REQUEST = 0x13; // 분식당 가격 등록 요청
    public static final byte PRICE_REGISTER_REGULAR_REQUEST = 0x14; // 학생/교직원 식당 가격 등록 요청

    // III. 관리자 - 정책/보고서 관리 (0x15 ~ 0x21)
    public static final byte COUPON_POLICY_LIST_REQUEST = 0x15;    // 쿠폰 정책 조회 요청
    public static final byte COUPON_POLICY_INSERT_REQUEST = 0x16;  // 쿠폰 정책 생성 요청
    public static final byte ORDER_PAYMENT_HISTORY_REQUEST = 0x17; // 주문/결제 내역 조회 요청 (기간별)
    public static final byte SALES_REPORT_REQUEST = 0x18;          // 매출 현황 조회 요청
    public static final byte USAGE_REPORT_REQUEST = 0x19;          // 이용 현황 조회 요청
    public static final byte CSV_SAMPLE_DOWNLOAD_REQUEST = 0x20;   // CSV 샘플 파일 다운로드 요청
    public static final byte CSV_MENU_UPLOAD_REQUEST = 0x21;       // CSV 파일 업로드 (메뉴 등록) 요청


    // -----------------------------------------------------------
    // Type: RESPONSE (서버 -> 클라이언트) - 0x30번대 사용
    // -----------------------------------------------------------
    // 요청에 대한 데이터 페이로드(DTO)를 포함하는 응답 코드입니다.
    public static final byte LOGIN_RESPONSE = 0x30;              // 로그인 성공 응답 (User DTO 포함)
    public static final byte SIGNUP_RESPONSE = 0x31;             // 회원가입 성공 응답 (성공 메시지)
    public static final byte MENU_LIST_RESPONSE = 0x32;          // 메뉴 목록 응답 (Menu DTO List 포함)
    public static final byte MENU_IMAGE_RESPONSE = 0x33;         // 메뉴 이미지 응답 (Raw Image Binary Data 포함)
    public static final byte COUPON_LIST_RESPONSE = 0x34;        // 쿠폰 목록 응답
    public static final byte COUPON_POLICY_LIST_RESPONSE = 0x35; // 쿠폰 정책 목록 응답
    public static final byte USAGE_HISTORY_RESPONSE = 0x36;      // 이용 내역 응답
    public static final byte ORDER_PAYMENT_HISTORY_RESPONSE = 0x37; // 주문/결제 내역 응답
    public static final byte SALES_REPORT_RESPONSE = 0x38;       // 매출 현황 보고서 응답
    public static final byte USAGE_REPORT_RESPONSE = 0x39;       // 이용 현황 보고서 응답
    public static final byte CSV_FILE_RESPONSE = 0x3A;           // CSV 파일 데이터 응답


    // -----------------------------------------------------------
    // Type: RESULT (범용 성공/실패/오류) - 0x50번대 사용
    // -----------------------------------------------------------
    // 모든 요청/응답에서 범용적으로 사용되어 최종 처리 결과를 나타냅니다.
    public static final byte SUCCESS = 0x50;                 // 성공 (작업 완료)
    public static final byte FAIL = 0x51;                 // 실패 (일반적인 DB 오류, 로직 오류)
    public static final byte INVALID_INPUT = 0x52;           // 유효성 검사 실패 / 잘못된 입력 값 (예: ID/PWD 불일치, DTO 필드 누락)
    public static final byte NOT_FOUND = 0x53;               // 데이터/파일을 찾을 수 없음 (예: 조회 가능한 메뉴 없음)
    public static final byte ALREADY_EXIST = 0x54;           // 이미 존재하는 항목 (예: 이미 가입된 ID, 중복 등록)
    public static final byte PERMISSION_DENIED = 0x55;       // 권한 없음 (예: 학생이 관리자 기능 요청)
    public static final byte SERVER_ERROR = 0x5F;            // 서버 내부 처리 중 치명적인 오류 발생 (최후의 수단)
}