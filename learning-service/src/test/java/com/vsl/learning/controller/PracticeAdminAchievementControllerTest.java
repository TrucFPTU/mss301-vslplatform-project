package com.vsl.learning.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsl.learning.dto.request.AchievementRequest;
import com.vsl.learning.dto.response.AdminAchievementResponse;
import com.vsl.learning.service.AchievementAdminService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PracticeAdminAchievementControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AchievementAdminService achievementAdminService;

    @InjectMocks
    private PracticeAdminAchievementController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/practice/admin/achievements -> 200 OK và danh sách thành tựu")
    void getAllAchievements_Returns200() throws Exception {
        AdminAchievementResponse item = AdminAchievementResponse.builder()
                .id(1L)
                .key("FIRST_STEP")
                .name("Bước Đầu")
                .description("Hoàn thành lần thực hành đầu tiên")
                .iconKey("directions_walk")
                .unlockedUserCount(12L)
                .build();

        when(achievementAdminService.getAllAchievements()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/practice/admin/achievements")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].key").value("FIRST_STEP"))
                .andExpect(jsonPath("$.data[0].unlockedUserCount").value(12L));

        verify(achievementAdminService).getAllAchievements();
    }

    @Test
    @DisplayName("GET /api/practice/admin/achievements/{id} -> 200 OK và chi tiết thành tựu")
    void getAchievementById_Returns200() throws Exception {
        AdminAchievementResponse item = AdminAchievementResponse.builder()
                .id(2L)
                .key("CORRECT_10")
                .name("Chính Xác")
                .description("Đạt 10 lần đánh giá đúng")
                .iconKey("check_circle")
                .unlockedUserCount(5L)
                .build();

        when(achievementAdminService.getAchievementById(2L)).thenReturn(item);

        mockMvc.perform(get("/api/practice/admin/achievements/{id}", 2L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(2L))
                .andExpect(jsonPath("$.data.name").value("Chính Xác"));

        verify(achievementAdminService).getAchievementById(2L);
    }

    @Test
    @DisplayName("POST /api/practice/admin/achievements -> 201 Created và tạo mới thành tựu")
    void createAchievement_Returns201() throws Exception {
        AchievementRequest request = AchievementRequest.builder()
                .key("STREAK_7")
                .name("Chăm Chỉ 7 Ngày")
                .description("Luyện tập 7 ngày liên tục")
                .iconKey("local_fire_department")
                .build();

        AdminAchievementResponse created = AdminAchievementResponse.builder()
                .id(9L)
                .key("STREAK_7")
                .name("Chăm Chỉ 7 Ngày")
                .description("Luyện tập 7 ngày liên tục")
                .iconKey("local_fire_department")
                .unlockedUserCount(0L)
                .build();

        when(achievementAdminService.createAchievement(any(AchievementRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/practice/admin/achievements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(9L))
                .andExpect(jsonPath("$.data.key").value("STREAK_7"))
                .andExpect(jsonPath("$.message").value("Tạo thành tựu thành công"));

        verify(achievementAdminService).createAchievement(any(AchievementRequest.class));
    }

    @Test
    @DisplayName("PUT /api/practice/admin/achievements/{id} -> 200 OK và cập nhật thành tựu")
    void updateAchievement_Returns200() throws Exception {
        AchievementRequest request = AchievementRequest.builder()
                .key("FIRST_STEP")
                .name("Bước Đầu Cập Nhật")
                .description("Mô tả mới")
                .iconKey("new_icon")
                .build();

        AdminAchievementResponse updated = AdminAchievementResponse.builder()
                .id(1L)
                .key("FIRST_STEP")
                .name("Bước Đầu Cập Nhật")
                .description("Mô tả mới")
                .iconKey("new_icon")
                .unlockedUserCount(8L)
                .build();

        when(achievementAdminService.updateAchievement(eq(1L), any(AchievementRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/practice/admin/achievements/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.name").value("Bước Đầu Cập Nhật"))
                .andExpect(jsonPath("$.message").value("Cập nhật thành tựu thành công"));

        verify(achievementAdminService).updateAchievement(eq(1L), any(AchievementRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/practice/admin/achievements/{id} -> 200 OK và xoá thành tựu")
    void deleteAchievement_Returns200() throws Exception {
        mockMvc.perform(delete("/api/practice/admin/achievements/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Xoá thành tựu thành công"));

        verify(achievementAdminService).deleteAchievement(1L);
    }

    @Test
    @DisplayName("POST /api/practice/admin/achievements/{id}/grant?userId=100 -> 200 OK trao tặng thành tựu")
    void grantAchievement_Returns200() throws Exception {
        mockMvc.perform(post("/api/practice/admin/achievements/{id}/grant", 1L)
                        .param("userId", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Trao tặng thành tựu cho học viên thành công"));

        verify(achievementAdminService).grantAchievementToUser(1L, 100L);
    }

    @Test
    @DisplayName("GET /api/achievements/admin -> route alias hoạt động tốt")
    void aliasRoute_WorksProperly() throws Exception {
        when(achievementAdminService.getAllAchievements()).thenReturn(List.of());

        mockMvc.perform(get("/api/achievements/admin")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }
}
