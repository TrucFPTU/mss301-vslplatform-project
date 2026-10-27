package com.vsl.learning.util;

import java.text.Normalizer;

/**
 * Chuẩn hóa văn bản tiếng Việt để SO KHỚP (không dùng để hiển thị). Bê từ monolith.
 * Dùng cho việc tìm từ vựng trùng (không phân biệt dấu / hoa-thường / cách trình bày).
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
}
