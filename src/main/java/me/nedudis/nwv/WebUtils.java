package me.nedudis.nwv;

import java.util.regex.Pattern;

public class WebUtils {
    private static final Pattern URL_PATTERN = Pattern.compile(
            "^https?://([\\\\w.-]+)(:[0-9]+)?(/[\\\\w./%+-]*)*(\\\\?[\\\\w.&%+-=]*)?(#\\\\w*)?$"
    );

    public static boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) return false;

        String cleanUrl = url.trim();

        if (!URL_PATTERN.matcher(cleanUrl).matches()) return false;

        String lowerUrl = cleanUrl.toLowerCase();

        return !lowerUrl.contains("localhost") &&
                !lowerUrl.contains("127.0.0.1") &&
                !lowerUrl.contains("192.168.") &&
                !lowerUrl.contains("10.") &&
                !lowerUrl.contains("172.16.");
    }
}
