package com.vsl.identity.service.saga;

import com.vsl.identity.client.SocialServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** social-service: gỡ theo dõi / chặn, xóa thông báo gửi tới người đã xóa, ẩn bài của họ. */
@Slf4j
@Component
@RequiredArgsConstructor
public class SocialUserDeletionParticipant implements UserDeletionParticipant {

    public static final String DESTINATION = "social-service";

    private final SocialServiceClient socialServiceClient;

    @Override
    public String destination() {
        return DESTINATION;
    }

    @Override
    public void cleanup(Long userId) {
        Object result = socialServiceClient.notifyUserDeleted(userId);
        log.info("social-service đã dọn dữ liệu của userId={}: {}", userId, result);
    }
}
