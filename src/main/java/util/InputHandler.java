package util;

import java.util.Scanner;

public class InputHandler {
    private static Scanner scanner = new Scanner(System.in);

    public static String getString(String prompt) {
        OutputHandler.showPrompt(prompt);
        return scanner.nextLine();
    }

    public static int getInt(String prompt) {
        OutputHandler.showPrompt(prompt);
        try {
            int value = scanner.nextInt();
            scanner.nextLine();
            return value;
        }
        catch (NumberFormatException e) {
            scanner = new Scanner(System.in);
            OutputHandler.showError("숫자를 입력하세요");
            return getInt(prompt);
        }
    }

    public static char getChar(String prompt) {
        OutputHandler.showPrompt(prompt);
        return scanner.nextLine().toUpperCase().charAt(0);
    }
}
