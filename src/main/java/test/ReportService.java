package test;

import network.Protocol;
import network.ProtocolCode;
import network.ProtocolType;
import persistence.dto.PaymentDTO;
import test.Util;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ReportService {

    // 메인에서 호출할 진입점
    public static void viewReports(NetworkClient nc) throws IOException {
        manageReports(nc);
    }

    // 관리자 메뉴 선택 루프
    private static void manageReports(NetworkClient nc) throws IOException {
        while (true) {
            System.out.println("\n--- [관리자 > 통계/보고서] ---");
            System.out.println(" 1. 식당별 결제 내역 상세");
            System.out.println(" 2. 식당별 매출 현황");
            System.out.println(" 3. 시간대별 이용 통계");
            System.out.println(" 0. 뒤로가기");
            System.out.print("선택>> ");
            int choice = Util.getIntInput();
            if (choice == 0) return;
            switch (choice) {
                case 1 -> orderPaymentHistory(nc);
                case 2 -> salesReport(nc);
                case 3 -> usageReport(nc);
                default -> System.out.println("잘못된 선택");
            }
        }
    }

    private static void orderPaymentHistory(NetworkClient nc) throws IOException {
        System.out.println("\n[관리자: 식당별 결제 내역 조회]");
        System.out.print("식당 ID: ");
        int rId = Util.getIntInput();
        nc.send(new Protocol(ProtocolType.REQUEST, ProtocolCode.ORDER_PAYMENT_HISTORY_REQUEST, rId));

        Protocol res = nc.receive();
        if (res.getCode() == ProtocolCode.ORDER_PAYMENT_HISTORY_RESPONSE) {
            List<PaymentDTO> list = (List<PaymentDTO>) res.getData();
            System.out.println("📜 결제 내역 (" + list.size() + "건):");
            for (PaymentDTO p : list) System.out.println("- " + p.getMenuName() + ", " + p.getMenuPriceAtTime() + "원");
        } else Util.printFail(res);
    }

    private static void salesReport(NetworkClient nc) throws IOException {
        nc.send(new Protocol(ProtocolType.REQUEST, ProtocolCode.SALES_REPORT_REQUEST, null));
        Protocol res = nc.receive();
        if (res.getCode() == ProtocolCode.SALES_REPORT_RESPONSE) {
            Map<String, Long> sales = (Map<String, Long>) res.getData();
            System.out.println("💰 식당별 매출: " + sales);
        } else Util.printFail(res);
    }

    private static void usageReport(NetworkClient nc) throws IOException {
        nc.send(new Protocol(ProtocolType.REQUEST, ProtocolCode.USAGE_REPORT_REQUEST, null));
        Protocol res = nc.receive();
        if (res.getCode() == ProtocolCode.TIME_STATS_RESPONSE) {
            List<String> stats = (List<String>) res.getData();
            System.out.println("📊 시간대별 통계:");
            stats.forEach(System.out::println);
        } else Util.printFail(res);
    }
}
