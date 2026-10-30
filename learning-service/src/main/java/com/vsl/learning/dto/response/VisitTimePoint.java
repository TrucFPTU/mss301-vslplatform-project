package com.vsl.learning.dto.response;

/** Một điểm trên biểu đồ lượt truy cập: label "2026-07-10" (ngày), "2026-07" (tháng), "2026" (năm). */
public record VisitTimePoint(String label, long count) {
}
