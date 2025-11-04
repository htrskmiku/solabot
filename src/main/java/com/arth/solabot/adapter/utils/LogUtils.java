package com.arth.solabot.adapter.utils;

public class LogUtils {

    private final static int MAX_LOG_LENGTH = 50;

    private LogUtils() {
    }

    public static String limitLen(String s) {
        return limitLen(s, MAX_LOG_LENGTH);
    }

    public static String limitLen(String s, int len) {
        if (s == null) return null;
        return s.length() <= len ? s : s.substring(0, len) + "...";
    }
}
