package com.vsl.learning.service;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.learning.dto.request.AchievementRequest;
import com.vsl.learning.dto.response.AdminAchievementResponse;
import com.vsl.learning.entity.Achievement;
import com.vsl.learning.entity.UserAchievement;
import com.vsl.learning.repository.AchievementRepository;
import com.vsl.learning.repository.UserAchievementRepository;
import com.vsl.learning.repository.projection.AchievementUnlockCountProjection;
import com.vsl.learning.service.impl.AchievementAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AchievementAdminServiceTest {

    @Mock
    private AchievementRepository achievementRepository;

    @Mock
    private UserAchievementRepository userAchievementRepository;

    @InjectMocks
    private AchievementAdminServiceImpl achievementAdminService;

    private Achievement sampleAchievement;

    @BeforeEach
    void setUp() {
        sampleAchievement = Achievement.builder()
                .id(1L)
                .key("FIRST_STEP")
                .name("Bước Đầu")
                .description("Hoàn thành lần thực hành đầu tiên")
                .iconKey("directions_walk")
                .build();
    }

    @Test
    @DisplayName("getAllAchievements -> trả về danh sách kèm số lượng mở khoá đúng")
    void getAllAchievements_Success() {
        AchievementUnlockCountProjection proj = mock(AchievementUnlockCountProjection.class);
        when(proj.getAchievementId()).thenReturn(1L);
        when(proj.getUnlockCount()).thenReturn(15L);

        when(userAchievementRepository.countUnlocksGroupByAchievement()).thenReturn(List.of(proj));
        when(achievementRepository.findAll()).thenReturn(List.of(sampleAchievement));

        List<AdminAchievementResponse> result = achievementAdminService.getAllAchievements();

        assertThat(result).hasSize(1);
        AdminAchievementResponse item = result.get(0);
        assertThat(item.getId()).isEqualTo(1L);
        assertThat(item.getKey()).isEqualTo("FIRST_STEP");
        assertThat(item.getUnlockedUserCount()).isEqualTo(15L);
    }

    @Test
    @DisplayName("getAchievementById -> trả về chi tiết thành tựu khi tồn tại")
    void getAchievementById_Success() {
        when(achievementRepository.findById(1L)).thenReturn(Optional.of(sampleAchievement));
        when(userAchievementRepository.countByAchievementId(1L)).thenReturn(5L);

        AdminAchievementResponse response = achievementAdminService.getAchievementById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Bước Đầu");
        assertThat(response.getUnlockedUserCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getAchievementById -> ném AppException NOT_FOUND khi không tồn tại")
    void getAchievementById_NotFound() {
        when(achievementRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> achievementAdminService.getAchievementById(99L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("createAchievement -> tạo thành công với ID tự động tăng")
    void createAchievement_Success() {
        AchievementRequest request = AchievementRequest.builder()
                .key("STREAK_7")
                .name("Chăm Chỉ 7 Ngày")
                .description("Luyện tập liên tục 7 ngày")
                .iconKey("local_fire_department")
                .build();

        when(achievementRepository.existsByKey("STREAK_7")).thenReturn(false);
        when(achievementRepository.findMaxId()).thenReturn(8L);
        when(achievementRepository.save(any(Achievement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminAchievementResponse created = achievementAdminService.createAchievement(request);

        assertThat(created.getId()).isEqualTo(9L);
        assertThat(created.getKey()).isEqualTo("STREAK_7");
        assertThat(created.getName()).isEqualTo("Chăm Chỉ 7 Ngày");
        assertThat(created.getUnlockedUserCount()).isEqualTo(0L);

        verify(achievementRepository).save(any(Achievement.class));
    }

    @Test
    @DisplayName("createAchievement -> ném VALIDATION_ERROR khi key bị trùng")
    void createAchievement_DuplicateKey() {
        AchievementRequest request = AchievementRequest.builder()
                .key("FIRST_STEP")
                .name("Bước Đầu Mới")
                .description("Mô tả")
                .build();

        when(achievementRepository.existsByKey("FIRST_STEP")).thenReturn(true);

        assertThatThrownBy(() -> achievementAdminService.createAchievement(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR);

        verify(achievementRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateAchievement -> cập nhật thông tin thành tựu thành công")
    void updateAchievement_Success() {
        AchievementRequest request = AchievementRequest.builder()
                .key("FIRST_STEP")
                .name("Bước Đầu Cập Nhật")
                .description("Mô tả mới")
                .iconKey("new_icon")
                .build();

        when(achievementRepository.findById(1L)).thenReturn(Optional.of(sampleAchievement));
        when(achievementRepository.save(any(Achievement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userAchievementRepository.countByAchievementId(1L)).thenReturn(10L);

        AdminAchievementResponse updated = achievementAdminService.updateAchievement(1L, request);

        assertThat(updated.getName()).isEqualTo("Bước Đầu Cập Nhật");
        assertThat(updated.getDescription()).isEqualTo("Mô tả mới");
        assertThat(updated.getIconKey()).isEqualTo("new_icon");
        assertThat(updated.getUnlockedUserCount()).isEqualTo(10L);
    }

    @Test
    @DisplayName("updateAchievement -> ném VALIDATION_ERROR khi đổi key sang một key đã tồn tại khác")
    void updateAchievement_DuplicateKey() {
        AchievementRequest request = AchievementRequest.builder()
                .key("EXISTING_KEY")
                .name("Tên")
                .description("Mô tả")
                .build();

        when(achievementRepository.findById(1L)).thenReturn(Optional.of(sampleAchievement));
        when(achievementRepository.existsByKey("EXISTING_KEY")).thenReturn(true);

        assertThatThrownBy(() -> achievementAdminService.updateAchievement(1L, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR);

        verify(achievementRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteAchievement -> xoá user_achievements liên kết và xoá thành tựu")
    void deleteAchievement_Success() {
        when(achievementRepository.findById(1L)).thenReturn(Optional.of(sampleAchievement));

        achievementAdminService.deleteAchievement(1L);

        verify(userAchievementRepository).deleteByAchievementId(1L);
        verify(achievementRepository).delete(sampleAchievement);
    }

    @Test
    @DisplayName("grantAchievementToUser -> mở khoá cho user nếu chưa có")
    void grantAchievementToUser_Success() {
        when(achievementRepository.findById(1L)).thenReturn(Optional.of(sampleAchievement));
        when(userAchievementRepository.existsByUserIdAndAchievementId(100L, 1L)).thenReturn(false);

        achievementAdminService.grantAchievementToUser(1L, 100L);

        verify(userAchievementRepository).save(any(UserAchievement.class));
    }

    @Test
    @DisplayName("grantAchievementToUser -> không làm gì nếu user đã có thành tựu này rồi")
    void grantAchievementToUser_AlreadyUnlocked() {
        when(achievementRepository.findById(1L)).thenReturn(Optional.of(sampleAchievement));
        when(userAchievementRepository.existsByUserIdAndAchievementId(100L, 1L)).thenReturn(true);

        achievementAdminService.grantAchievementToUser(1L, 100L);

        verify(userAchievementRepository, never()).save(any(UserAchievement.class));
    }
}
