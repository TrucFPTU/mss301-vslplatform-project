package com.vsl.common.text;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;

/**
 * Chuẩn hóa văn bản tiếng Việt để SO KHỚP (không dùng để hiển thị). Bê từ monolith.
 * Dùng chung: learning (tìm từ vựng trùng) và social (chặn đăng lại bài trùng).
 */
public final class VietnameseText {

    private VietnameseText() {
    }

    /**
     * Bỏ dấu tiếng Việt + hạ chữ thường + gom khoảng trắng.
     * NFD tách dấu thanh rồi xóa; riêng đ/ơ/ư không tách bằng NFD nên map tay.
     * Ví dụ: "con chuột" -> "con chuot", "Sợ  Hãi" -> "so hai".
     */
    public static String fold(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .replace('ơ', 'o').replace('Ơ', 'O')
                .replace('ư', 'u').replace('Ư', 'U');
        return n.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    /** Chuẩn hóa để LƯU/HIỂN THỊ: chỉ trim và gom khoảng trắng, giữ nguyên dấu. */
    public static String normalize(String word) {
        return word == null ? "" : word.trim().replaceAll("\\s+", " ");
    }

    /**
     * Dấu vân tay SHA-256 (hex, 64 ký tự) của văn bản đã {@link #fold}: hai văn bản chỉ khác dấu /
     * hoa-thường / khoảng trắng cho cùng kết quả. Lưu cột này để so trùng bằng truy vấn có index.
     */
    public static String fingerprint(String s) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(fold(s).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 không khả dụng", e);
        }
    }
}
