package com.webscreen;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class UrlUtil {
    private UrlUtil() {}

    public static boolean isSafe(String url) {
        if (url == null || url.length() > 2048) return false;
        String l = url.toLowerCase(Locale.ROOT);
        return l.startsWith("http://") || l.startsWith("https://");
    }

    /** Turns whatever was typed into a URL (adds https://, or searches Google). */
    public static String normalize(String input) {
        String u = input == null ? "" : input.trim();
        if (u.isEmpty()) return u;
        if (!u.contains("://")) {
            if (u.contains(" ") || !(u.contains(".") || u.startsWith("localhost"))) {
                u = "https://www.google.com/search?q=" + URLEncoder.encode(u, StandardCharsets.UTF_8);
            } else {
                u = "https://" + u;
            }
        }
        return u;
    }
}
