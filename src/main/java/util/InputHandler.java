package util;

import java.util.Scanner;

public class InputHandler {
    private static Scanner scanner = new Scanner(System.in);

    public static String getString(String prompt) {
        OutputHandler.showPrompt(prompt);
        return scanner.nextLine();
    }

    public static int getInt(String prompt) {
        while (true) {
            OutputHandler.showPrompt(prompt);

            if (scanner.hasNextInt()) {
                int value = scanner.nextInt();
                scanner.nextLine();
                return value;
            }
            else {
                scanner.nextLine();
                OutputHandler.showError("숫자를 입력하세요");
            }
        }
    }


    public static char getChar(String prompt) {
        while (true) {
            OutputHandler.showPrompt(prompt);
            String input = scanner.nextLine().trim().toUpperCase();

            if (input.length() == 1 && Character.isLetter(input.charAt(0))) {
                return input.charAt(0);
            }

            OutputHandler.showError("문자를 입력해주세요");
        }
    }

}
