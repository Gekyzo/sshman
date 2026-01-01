package com.sshman.utils;

public class Strings {

    public static String truncate(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    public static String wrapText(String text, int width) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            int end = Math.min(i + width, text.length());
            sb.append(text, i, end);
            if (end < text.length()) {
                sb.append("\n");
            }
            i = end;
        }
        return sb.toString();
    }

}
