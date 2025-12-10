package network;

public class ProtocolCode {
    // @=$& 파싱용

    // ----------------------------------------------------
    // 1. 요청 코드 (REQUEST, ProtocolType 0x01) : 클라이언트 -> 서버
    // ----------------------------------------------------

    // 공통 기능
    public static final byte LOGIN_REQUEST = 0x10;          // 로그인 요청
    public static final byte LOGOUT_REQUEST = 0x1F;         // 로그아웃 요청

    // 사용자(학생/교직원) 기능
    public static final byte MENU_QUERY_REQUEST = 0x20;       // 메뉴 조회(텍스트) 요청
    public static final byte IMAGE_DOWNLOAD_REQUEST = 0x21;   // 메뉴 이미지 다운로드 요청
    public static final byte PURCHASE_COUPON_REQUEST = 0x22;  // 쿠폰 구매 요청
    public static final byte PAYMENT_REQUEST = 0x23;          // 결제 요청 (카드/쿠폰)
    public static final byte USAGE_HISTORY_REQUEST = 0x24;    // 이용 내역 조회 요청

    // 관리자 기능
    public static final byte ADMIN_MENU_REGISTER_REQUEST = 0x30;    // 메뉴 등록/수정 요청
    public static final byte ADMIN_IMAGE_UPLOAD_REQUEST = 0x31;     // 메뉴 사진 업로드 요청
    public static final byte ADMIN_PRICE_REGISTER_REQUEST = 0x32;   // 식당별 가격 등록 요청
    public static final byte ADMIN_POLICY_REGISTER_REQUEST = 0x33;  // 쿠폰 정책 관리 요청
    public static final byte ADMIN_SALES_QUERY_REQUEST = 0x34;      // 매출 현황/이용 현황 조회 요청
    public static final byte ADMIN_HISTORY_BY_RESTAURANT_REQUEST = 0x35; // 식당별 결제 내역
    public static final byte ADMIN_HISTORY_BY_PERIOD_REQUEST = 0x36;     // 기간별 결제 내역
    public static final byte ADMIN_TIME_STATS_REQUEST = 0x37;            // 시간대별 통계

    // ----------------------------------------------------
    // 2. 응답 코드 (RESPONSE, ProtocolType 0x02) : 서버 -> 클라이언트 (데이터 포함)
    // ----------------------------------------------------

    public static final byte LOGIN_RESPONSE = (byte) 0x80;          // 로그인 성공 시 사용자 DTO 반환
    public static final byte MENU_QUERY_RESPONSE = (byte) 0x81;     // 메뉴 목록 DTO 반환
    public static final byte IMAGE_DOWNLOAD_RESPONSE = (byte) 0x82; // 이미지 파일(byte[]) 데이터 반환
    public static final byte USAGE_HISTORY_RESPONSE = (byte) 0x83;  // 이용 내역 DTO 목록 반환
    public static final byte ADMIN_SALES_QUERY_RESPONSE = (byte) 0x84; // 통계 데이터 DTO 반환


    // ----------------------------------------------------
    // 3. 결과 코드 (RESULT, ProtocolType 0x03) : 서버 -> 클라이언트 (상태만 알림)
    // ----------------------------------------------------

    public static final byte SUCCESS = (byte) 0xF0;             // 요청/처리 성공
    public static final byte FAIL = (byte) 0xF1;                // 일반적인 처리 실패
    public static final byte INVALID_CREDENTIALS = (byte) 0xF2; // 로그인 실패 (ID/PW 불일치)
    public static final byte ACCESS_DENIED = (byte) 0xF3;       // 권한 없음 (예: 학생이 관리자 기능 요청 시)
    public static final byte ADDITIONAL_FEE_REQUIRED = (byte) 0xF4; // 결제 시 추가금 필요
    public static final byte SERVER_ERROR = (byte) 0xFF;        // 서버 측 심각한 오류 발생
}