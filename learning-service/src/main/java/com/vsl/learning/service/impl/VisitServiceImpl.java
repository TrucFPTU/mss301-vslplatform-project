package com.vsl.learning.service.impl;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.common.response.PageResponse;
import com.vsl.learning.client.IdentityServiceClient;
import com.vsl.learning.client.SocialServiceClient;
import com.vsl.learning.dto.remote.RemoteUserStats;
import com.vsl.learning.dto.response.AdminSummaryResponse;
import com.vsl.learning.dto.response.VisitLogResponse;
import com.vsl.learning.dto.response.VisitTimePoint;
import com.vsl.learning.entity.VisitLog;
import com.vsl.learning.repository.VisitLogRepository;
import com.vsl.learning.repository.VocabularyRepository;
import com.vsl.learning.service.VisitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ghi nhận + thống kê lượt truy cập. Bê từ monolith.
 * Khác bản gốc: số học viên (identity) và số blog (social) lấy qua Eureka; log chỉ trả userId.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitServiceImpl implements VisitService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    /** Chặn khoảng ngày quá lớn ở mức DAY (biểu đồ tối đa ~5 năm theo ngày). */
    private static final long MAX_DAY_POINTS = 1900;

    private final VisitLogRepository visitLogRepository;
    private final VocabularyRepository vocabularyRepository;
    private final IdentityServiceClient identityServiceClient;
    private final SocialServiceClient socialServiceClient;

    // ──────────────────────── GHI LƯỢT TRUY CẬP ────────────────────────

    @Async("visitTaskExecutor")
    @Override
    @Transactional
    public void recordVisit(Long userId, String sessionId, String ipAddress, String userAgent) {
        try {
            // Upsert theo phiên: phiên đã có 1 lượt (vd lúc mở app là GUEST) và giờ khách vừa đăng nhập
            // -> chỉ GẮN user vào dòng cũ, giữ thời điểm truy cập đầu. Tránh đếm trùng 1 người.
            if (sessionId != null && !sessionId.isBlank()) {
                VisitLog existing = visitLogRepository.findFirstBySessionId(sessionId).orElse(null);
                if (existing != null) {
                    if (userId != null && existing.getUserId() == null) {
                        existing.setUserId(userId);
                        visitLogRepository.save(existing);
                    }
                    return; // Đã có lượt cho phiên này -> không ghi trùng
                }
            }

            visitLogRepository.save(VisitLog.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .deviceInfo(parseDevice(userAgent))
                    .ipAddress(ipAddress)
                    .location(resolveLocation(ipAddress))
                    .visitedAt(LocalDateTime.now())
                    .build());
        } catch (Exception ex) {
            // Tracking không được làm vỡ trải nghiệm người dùng -> chỉ log
            log.warn("Không ghi được lượt truy cập: {}", ex.getMessage());
        }
    }

    /** Phân tích User-Agent thành chuỗi "OS · Trình duyệt" đơn giản. */
    private String parseDevice(String ua) {
        if (ua == null || ua.isBlank()) return "Không xác định";
        String s = ua.toLowerCase();

        String os;
        if (s.contains("android")) os = "Android";
        else if (s.contains("iphone") || s.contains("ipad") || s.contains("ios")) os = "iOS";
        else if (s.contains("windows")) os = "Windows";
        else if (s.contains("mac os") || s.contains("macintosh")) os = "macOS";
        else if (s.contains("linux")) os = "Linux";
        else os = "Khác";

        String browser;
        if (s.contains("edg")) browser = "Edge";
        else if (s.contains("opr") || s.contains("opera")) browser = "Opera";
        else if (s.contains("chrome")) browser = "Chrome";
        else if (s.contains("firefox")) browser = "Firefox";
        else if (s.contains("safari")) browser = "Safari";
        else browser = "Trình duyệt khác";

        boolean mobile = s.contains("mobile") || s.contains("android") || s.contains("iphone");
        return os + " · " + browser + (mobile ? " · Di động" : "");
    }

    /** Suy ra vị trí từ IP (best-effort qua ip-api.com). IP nội bộ -> "Cục bộ (Local)". Lỗi -> null. */
    private String resolveLocation(String ip) {
        if (ip == null || ip.isBlank() || isPrivateIp(ip)) {
            return "Cục bộ (Local)";
        }
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(1500)).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://ip-api.com/json/" + ip + "?fields=status,country,regionName,city"))
                    .timeout(Duration.ofMillis(1500))
                    .GET()
                    .build();
            String body = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
            if (body == null || !body.contains("\"status\":\"success\"")) return null;
            String city = extractJson(body, "city");
            String region = extractJson(body, "regionName");
            String country = extractJson(body, "country");
            List<String> parts = new ArrayList<>();
            if (city != null && !city.isBlank()) parts.add(city);
            else if (region != null && !region.isBlank()) parts.add(region);
            if (country != null && !country.isBlank()) parts.add(country);
            return parts.isEmpty() ? null : String.join(", ", parts);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception ex) {
            log.debug("Tra vị trí IP {} thất bại: {}", ip, ex.getMessage());
            return null;
        }
    }

    private boolean isPrivateIp(String ip) {
        if (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1") || ip.equals("::1")
                || ip.equalsIgnoreCase("localhost") || ip.startsWith("10.") || ip.startsWith("192.168.")) {
            return true;
        }
        // 172.16.0.0 – 172.31.255.255 (dải Docker / LAN)
        if (ip.startsWith("172.")) {
            String[] parts = ip.split("\\.");
            if (parts.length > 1) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        return false;
    }

    /** Rút giá trị chuỗi thô từ JSON phẳng (không dùng thư viện để tránh phụ thuộc). */
    private String extractJson(String json, String key) {
        String needle = "\"" + key + "\":\"";
        int i = json.indexOf(needle);
        if (i < 0) return null;
        int start = i + needle.length();
        int end = json.indexOf('"', start);
        return end < 0 ? null : json.substring(start, end);
    }

    // ──────────────────────── TỔNG QUAN ────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AdminSummaryResponse getSummary() {
        RemoteUserStats userStats = identityServiceClient.getUserStats();
        return new AdminSummaryResponse(
                userStats != null ? userStats.totalStudents() : null,
                visitLogRepository.count(),
                vocabularyRepository.count(),
                // Chỉ đếm bài đã xuất bản (PUBLISHED), không tính nháp / đã gỡ
                socialServiceClient.countPublishedBlogs());
    }

    // ──────────────────────── BIỂU ĐỒ THEO THỜI GIAN ────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<VisitTimePoint> getVisitTimeSeries(LocalDate from, LocalDate to, String granularity) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Khoảng ngày không hợp lệ (from phải trước hoặc bằng to)");
        }
        String gran = granularity == null ? "DAY" : granularity.toUpperCase();
        if (!List.of("DAY", "MONTH", "YEAR").contains(gran)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "granularity chỉ nhận DAY, MONTH hoặc YEAR");
        }
        if ("DAY".equals(gran) && ChronoUnit.DAYS.between(from, to) > MAX_DAY_POINTS) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Khoảng ngày quá dài cho mức DAY, hãy dùng MONTH hoặc YEAR");
        }

        List<LocalDateTime> timestamps = visitLogRepository.findVisitedAtBetween(
                from.atStartOfDay(), to.plusDays(1).atStartOfDay()); // bao gồm hết ngày 'to'

        Map<String, Long> counts = new LinkedHashMap<>();
        for (LocalDateTime ts : timestamps) {
            counts.merge(bucketLabel(ts.toLocalDate(), gran), 1L, Long::sum);
        }

        // Lấp đầy khoảng trống để biểu đồ liên tục
        List<VisitTimePoint> points = new ArrayList<>();
        for (LocalDate cursor = truncate(from, gran); !cursor.isAfter(to); cursor = increment(cursor, gran)) {
            String key = bucketLabel(cursor, gran);
            points.add(new VisitTimePoint(key, counts.getOrDefault(key, 0L)));
        }
        return points;
    }

    private String bucketLabel(LocalDate date, String gran) {
        return switch (gran) {
            case "YEAR" -> String.valueOf(date.getYear());
            case "MONTH" -> date.format(MONTH_FMT);
            default -> date.format(DAY_FMT);
        };
    }

    private LocalDate truncate(LocalDate date, String gran) {
        return switch (gran) {
            case "YEAR" -> date.withDayOfYear(1);
            case "MONTH" -> date.withDayOfMonth(1);
            default -> date;
        };
    }

    private LocalDate increment(LocalDate date, String gran) {
        return switch (gran) {
            case "YEAR" -> date.plusYears(1);
            case "MONTH" -> date.plusMonths(1);
            default -> date.plusDays(1);
        };
    }

    // ──────────────────────── BẢNG LOG CHI TIẾT ────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VisitLogResponse> getVisitLogs(int page, int size) {
        return Paging.toPageResponse(visitLogRepository
                .findAllByOrderByVisitedAtDesc(PageRequest.of(Paging.safePage(page), Paging.safeSize(size)))
                .map(this::toResponse));
    }

    private VisitLogResponse toResponse(VisitLog v) {
        return new VisitLogResponse(
                v.getId(),
                v.getUserId() != null ? "USER" : "GUEST",
                v.getUserId(),
                v.getDeviceInfo(),
                v.getIpAddress(),
                v.getLocation(),
                v.getVisitedAt());
    }
}
