package com.vsl.learning.service;

import com.vsl.common.exception.AppException;
import com.vsl.learning.client.IdentityServiceClient;
import com.vsl.learning.client.SocialServiceClient;
import com.vsl.learning.dto.remote.RemoteUserStats;
import com.vsl.learning.dto.response.AdminSummaryResponse;
import com.vsl.learning.dto.response.VisitTimePoint;
import com.vsl.learning.entity.VisitLog;
import com.vsl.learning.repository.VisitLogRepository;
import com.vsl.learning.repository.VocabularyRepository;
import com.vsl.learning.service.impl.VisitServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisitServiceTest {

    @Mock
    private VisitLogRepository visitLogRepository;
    @Mock
    private VocabularyRepository vocabularyRepository;
    @Mock
    private IdentityServiceClient identityServiceClient;
    @Mock
    private SocialServiceClient socialServiceClient;

    @InjectMocks
    private VisitServiceImpl visitService;

    @Test
    @DisplayName("recordVisit: phiên mới -> ghi 1 dòng, IP nội bộ -> vị trí 'Cục bộ (Local)'")
    void recordVisit_NewSession_SavesRow() {
        when(visitLogRepository.findFirstBySessionId("s1")).thenReturn(Optional.empty());

        visitService.recordVisit(null, "s1", "127.0.0.1", "Mozilla/5.0 (Windows NT 10.0) Chrome/120");

        ArgumentCaptor<VisitLog> captor = ArgumentCaptor.forClass(VisitLog.class);
        verify(visitLogRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isNull();
        assertThat(captor.getValue().getLocation()).isEqualTo("Cục bộ (Local)");
        assertThat(captor.getValue().getDeviceInfo()).isEqualTo("Windows · Chrome");
    }

    @Test
    @DisplayName("recordVisit: phiên đã có dòng GUEST và user vừa đăng nhập -> gắn userId, không tạo dòng mới")
    void recordVisit_ExistingGuestSession_UpgradesToUser() {
        VisitLog guest = VisitLog.builder().id(5L).sessionId("s1").visitedAt(LocalDateTime.now()).build();
        when(visitLogRepository.findFirstBySessionId("s1")).thenReturn(Optional.of(guest));

        visitService.recordVisit(42L, "s1", "127.0.0.1", null);

        assertThat(guest.getUserId()).isEqualTo(42L);
        verify(visitLogRepository, times(1)).save(guest);
    }

    @Test
    @DisplayName("recordVisit: phiên đã có user -> không ghi gì thêm")
    void recordVisit_ExistingUserSession_NoWrite() {
        VisitLog row = VisitLog.builder().id(5L).userId(7L).sessionId("s1").visitedAt(LocalDateTime.now()).build();
        when(visitLogRepository.findFirstBySessionId("s1")).thenReturn(Optional.of(row));

        visitService.recordVisit(7L, "s1", "127.0.0.1", null);

        verify(visitLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("getVisitTimeSeries DAY: lấp ngày trống bằng 0")
    void timeSeries_Day_FillsGaps() {
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 3);
        when(visitLogRepository.findVisitedAtBetween(any(), any())).thenReturn(List.of(
                LocalDateTime.of(2026, 7, 1, 9, 0),
                LocalDateTime.of(2026, 7, 1, 10, 0),
                LocalDateTime.of(2026, 7, 3, 23, 59)));

        List<VisitTimePoint> points = visitService.getVisitTimeSeries(from, to, "day");

        assertThat(points).containsExactly(
                new VisitTimePoint("2026-07-01", 2),
                new VisitTimePoint("2026-07-02", 0),
                new VisitTimePoint("2026-07-03", 1));
    }

    @Test
    @DisplayName("getVisitTimeSeries MONTH: gom theo tháng")
    void timeSeries_Month_GroupsByMonth() {
        when(visitLogRepository.findVisitedAtBetween(any(), any())).thenReturn(List.of(
                LocalDateTime.of(2026, 1, 15, 8, 0),
                LocalDateTime.of(2026, 3, 2, 8, 0)));

        List<VisitTimePoint> points = visitService.getVisitTimeSeries(
                LocalDate.of(2026, 1, 20), LocalDate.of(2026, 3, 5), "MONTH");

        assertThat(points).containsExactly(
                new VisitTimePoint("2026-01", 1),
                new VisitTimePoint("2026-02", 0),
                new VisitTimePoint("2026-03", 1));
    }

    @Test
    @DisplayName("getVisitTimeSeries: from sau to hoặc granularity sai -> VALIDATION_ERROR")
    void timeSeries_InvalidInput_Throws() {
        assertThrows(AppException.class, () -> visitService.getVisitTimeSeries(
                LocalDate.of(2026, 7, 5), LocalDate.of(2026, 7, 1), "DAY"));
        assertThrows(AppException.class, () -> visitService.getVisitTimeSeries(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5), "WEEK"));
    }

    @Test
    @DisplayName("getSummary: identity/social không phản hồi -> trường tương ứng null, không lỗi")
    void summary_RemoteDown_ReturnsNullFields() {
        when(identityServiceClient.getUserStats()).thenReturn(null);
        when(socialServiceClient.countPublishedBlogs()).thenReturn(null);
        when(visitLogRepository.count()).thenReturn(10L);
        when(vocabularyRepository.count()).thenReturn(43L);

        AdminSummaryResponse summary = visitService.getSummary();

        assertThat(summary.totalStudents()).isNull();
        assertThat(summary.totalBlogs()).isNull();
        assertThat(summary.totalVisits()).isEqualTo(10L);
        assertThat(summary.totalVocabularies()).isEqualTo(43L);
    }

    @Test
    @DisplayName("getSummary: lấy số học viên từ identity, số blog từ social")
    void summary_RemoteUp_UsesRemoteCounts() {
        when(identityServiceClient.getUserStats()).thenReturn(new RemoteUserStats(50, 45));
        when(socialServiceClient.countPublishedBlogs()).thenReturn(12L);

        AdminSummaryResponse summary = visitService.getSummary();

        assertThat(summary.totalStudents()).isEqualTo(45L);
        assertThat(summary.totalBlogs()).isEqualTo(12L);
    }
}
