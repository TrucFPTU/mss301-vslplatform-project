package com.vsl.identity.dto.request;

import com.vsl.identity.entity.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusUpdateRequest {

    @NotNull(message = "Trạng thái người dùng không được để trống")
    private UserStatus status;

    private String reason;
}
