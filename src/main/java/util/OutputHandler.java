package util;

public class OutputHandler {
    public static void showDoubleBar() {
        System.out.println("====================================================");
    }
    public static void showSingleBar() {
        System.out.println("----------------------------------------------------");
    }

    public static void showTitle(String title) {
        showDoubleBar();
        System.out.println(" \uD83D\uDCC1 " + title);
    }

    public static void showMenu(int num, String message) {
        System.out.printf(" %2d. %s\n", num, message);
    }

    public static void showMessage(String message) {
        System.out.print(message);
    }

    public static void showIn(String message) {
        showSingleBar();
        System.out.print(" ▶ " + message);
    }

    public static void showOut(String message) {
        showSingleBar();
        System.out.println(" ◀ " + message + "\n");
    }

    public static void showSuccess(String message) {
        showSingleBar();
        System.out.println(" ✔ " + message+ "\n");
    }

    public static void showFail(String message) {
        showSingleBar();
        System.out.println(" ✖ " + message + "\n");
    }
}
