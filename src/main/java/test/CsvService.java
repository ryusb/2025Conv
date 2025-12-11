package test;

import network.Protocol;
import network.ProtocolCode;
import network.ProtocolType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CsvService {
    // 메인에서 호출할 진입점
    public static void manageData(NetworkClient nc) throws IOException {
        manageCsv(nc);
    }

    // 관리자 CSV 메뉴 루프
    private static void manageCsv(NetworkClient nc) throws IOException {
        while (true) {
            System.out.println("\n--- [관리자 > CSV 데이터] ---");
            System.out.println(" 1. 샘플 파일 다운로드");
            System.out.println(" 2. 메뉴 일괄 업로드 (CSV)");
            System.out.println("0. 뒤로가기");
            System.out.print("선택>> ");
            int choice = Util.getIntInput();
            if (choice == 0) return;
            switch (choice) {
                case 1 -> downloadSample(nc);
                case 2 -> uploadCsv(nc);
                default -> System.out.println("잘못된 선택");
            }
        }
    }

    private static void downloadSample(NetworkClient nc) throws IOException {
        nc.send(new Protocol(ProtocolType.REQUEST, ProtocolCode.CSV_SAMPLE_DOWNLOAD_REQUEST, null));
        Protocol res = nc.receive();
        if (res.getCode() == ProtocolCode.CSV_FILE_RESPONSE) {
            byte[] data = (byte[]) res.getData();
            Files.write(Paths.get("sample.csv"), data);
            System.out.println("✅ sample.csv 다운로드 완료");
        } else Util.printFail(res);
    }

    private static void uploadCsv(NetworkClient nc) throws IOException {
        System.out.println("\n[관리자: CSV 업로드]");
        System.out.print("업로드할 CSV 파일 경로: ");
        String path = new java.util.Scanner(System.in).nextLine();
        try {
            byte[] data = Files.readAllBytes(Paths.get(path));
            nc.send(new Protocol(ProtocolType.REQUEST, ProtocolCode.CSV_MENU_UPLOAD_REQUEST, data));
            Util.printSimpleResult(nc.receive());
        } catch (Exception e) {
            System.out.println("파일 에러: " + e.getMessage());
        }
    }
}
