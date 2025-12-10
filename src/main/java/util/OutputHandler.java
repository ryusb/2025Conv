package util;

public class OutputHandler {
    public static void showBar() {
        System.out.println("----------------------------------------------------");
    }

    public static void showTitle(String title) {
        System.out.println("[ " + title + " ]");
    }

    public static void showMenu(int num, String message) {
        System.out.printf("%02d. %s\n", num, message);
    }

    public static void showMessage(String message) {
        System.out.println(message);
    }

    public static void showPrompt(String message) {
        System.out.print(">> " + message + " : ");
    }

    public static void showSuccess(String message) {
        System.out.println(">> 성공 : " + message);
    }

    public static void showError(String message) {
        System.out.println(">> 오류 : " + message);
    }
}
