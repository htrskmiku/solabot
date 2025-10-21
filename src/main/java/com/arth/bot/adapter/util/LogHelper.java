package com.arth.bot.adapter.util;

public class LogHelper {

    private final static int MAX_LOG_LENGTH = 50;

    public static String limitLen(String s) {
        return limitLen(s, MAX_LOG_LENGTH);
    }

    public static String limitLen(String s, int len) {
        if (s == null) return null;
        return s.length() <= len ? s : s.substring(0, len) + "...";
    }
}
