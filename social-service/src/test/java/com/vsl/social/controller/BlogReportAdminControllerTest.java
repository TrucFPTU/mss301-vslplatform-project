package com.vsl.social.controller;

import com.vsl.common.response.PageResponse;
import com.vsl.social.dto.ReportDetailResponse;
import com.vsl.social.dto.ReportResponse;
import com.vsl.social.dto.UserBriefResponse;
import com.vsl.social.entity.BlogStatus;
import com.vsl.social.entity.ReportStatus;
import com.vsl.social.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BlogReportAdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReportService reportService;

    @InjectMocks
    private BlogReportAdminController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/blogs/admin/reports -> 200 OK va danh sach phan trang")
    void listReports_Returns200AndPageResponse() throws Exception {
        Instant now = Instant.now();
        ReportResponse item = new ReportResponse(1L, 10L, "Bài viết vi phạm", BlogStatus.PUBLISHED, new UserBriefResponse(100L, "reporter", "Người Báo", null, "Người Báo"), "Quảng cáo rác (SPAM)", ReportStatus.PENDING, now, now);

        PageResponse<ReportResponse> pageResponse = PageResponse.<ReportResponse>builder()
                .content(List.of(item))
                .page(0)
                .size(10)
                .totalElements(1L)
                .totalPages(1)
                .last(true)
                .build();

        when(reportService.listReports(eq(ReportStatus.PENDING), eq(0), eq(10)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/blogs/admin/reports")
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andExpect(jsonPath("$.data.content[0].blogTitle").value("Bài viết vi phạm"))
                .andExpect(jsonPath("$.data.content[0].reason").value("Quảng cáo rác (SPAM)"))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));

        verify(reportService).listReports(eq(ReportStatus.PENDING), eq(0), eq(10));
    }

    @Test
    @DisplayName("GET /api/blogs/admin/reports/{id} -> 200 OK va chi tiet kem blog")
    void getReportDetail_Returns200() throws Exception {
        Instant now = Instant.now();
        ReportDetailResponse detail = new ReportDetailResponse(1L, 10L, "Bài viết vi phạm", "Nội dung vi phạm xúc phạm cá nhân...", null, BlogStatus.PUBLISHED, null, new UserBriefResponse(200L, "author", "Tác Giả", null, "Tác Giả"), new UserBriefResponse(100L, "reporter", "Người Báo", null, "Người Báo"), "Xúc phạm cá nhân", ReportStatus.PENDING, now, now);

        when(reportService.getReportDetail(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/blogs/admin/reports/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.blogTitle").value("Bài viết vi phạm"))
                .andExpect(jsonPath("$.data.blogStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.blogContent").value("Nội dung vi phạm xúc phạm cá nhân..."));

        verify(reportService).getReportDetail(1L);
    }

    @Test
    @DisplayName("PUT /api/blogs/admin/reports/{id}/resolve -> 200 OK va trang thai RESOLVED")
    void resolve_Returns200() throws Exception {
        Instant now = Instant.now();
        ReportResponse resolved = new ReportResponse(1L, 10L, "Bài viết đã bị ẩn", BlogStatus.PUBLISHED, new UserBriefResponse(100L, "reporter", "Người Báo", null, "Người Báo"), "Nội dung nhạy cảm", ReportStatus.RESOLVED, now, now);

        when(reportService.resolve(eq(1L), any(), eq("Spam"))).thenReturn(resolved);

        mockMvc.perform(put("/api/blogs/admin/reports/{id}/resolve", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Spam\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.message").value("Đã xử lý vi phạm và gỡ bài viết"));

        verify(reportService).resolve(eq(1L), any(), eq("Spam"));
    }

    @Test
    @DisplayName("PUT /api/blogs/admin/reports/{id}/reject -> 200 OK va trang thai REJECTED")
    void reject_Returns200() throws Exception {
        Instant now = Instant.now();
        ReportResponse rejected = new ReportResponse(1L, 10L, "Bài viết hợp lệ", BlogStatus.PUBLISHED, new UserBriefResponse(100L, "reporter", "Người Báo", null, "Người Báo"), "Báo cáo sai sự thật", ReportStatus.REJECTED, now, now);

        when(reportService.reject(1L)).thenReturn(rejected);

        mockMvc.perform(put("/api/blogs/admin/reports/{id}/reject", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.message").value("Đã bác bỏ báo cáo vi phạm"));

        verify(reportService).reject(1L);
    }

    @Test
    @DisplayName("PUT /api/blogs/admin/reports/{id}/resolve thiếu lý do -> 400, không gọi service")
    void resolve_WithoutReason_Returns400() throws Exception {
        mockMvc.perform(put("/api/blogs/admin/reports/{id}/resolve", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"  \"}"))
                .andExpect(status().isBadRequest());
        verify(reportService, never()).resolve(any(), any(), any());
    }

    @Test
    @DisplayName("GET /api/blogs/admin/reports/pending-count -> số báo cáo chờ")
    void pendingCount_Returns200() throws Exception {
        when(reportService.countPending()).thenReturn(4L);
        mockMvc.perform(get("/api/blogs/admin/reports/pending-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(4));
    }
}
