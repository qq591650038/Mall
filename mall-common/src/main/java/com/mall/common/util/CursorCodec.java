package com.mall.common.util;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public final class CursorCodec {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    private CursorCodec() {
    }

    public static String encode(LocalDateTime createTime, Long id) {
        if (createTime == null || id == null) return null;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                (FORMATTER.format(createTime) + "|" + id).getBytes(StandardCharsets.UTF_8));
    }

    public static Decoded decode(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            String value = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = value.split("\\|", 2);
            if (parts.length != 2) return null;
            return new Decoded(LocalDateTime.parse(parts[0], FORMATTER), Long.parseLong(parts[1]));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public record Decoded(LocalDateTime createTime, Long id) {
    }
}
