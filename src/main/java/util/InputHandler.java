package util;

import java.util.Scanner;

public class InputHandler {
    private static Scanner scanner = new Scanner(System.in);

    public static String getString(String prompt) {
        OutputHandler.showIn(prompt);
        String input = scanner.nextLine();
        System.out.println();
        return input;
    }

    public static String getLogin(String prompt) {
        OutputHandler.showMessage(prompt);
        String input = scanner.nextLine();
        return input;
    }

    public static int getInt(String prompt) {
        while (true) {
            OutputHandler.showIn(prompt);

            if (scanner.hasNextInt()) {
                int value = scanner.nextInt();
                scanner.nextLine();
                System.out.println();
                return value;
            }
            else {
                OutputHandler.showFail("숫자를 입력하세요");
                scanner.nextLine();
            }
        }
    }


    public static char getChar(String prompt) {
        while (true) {
            OutputHandler.showIn(prompt);
            String input = scanner.nextLine().trim().toUpperCase();
            System.out.println();

            if (input.length() == 1 && Character.isLetter(input.charAt(0))) {
                return input.charAt(0);
            }

            OutputHandler.showFail("문자를 입력해주세요");
        }
    }

}
