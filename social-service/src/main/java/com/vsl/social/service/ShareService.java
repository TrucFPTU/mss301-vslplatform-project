package com.vsl.social.service;

import com.vsl.social.dto.ShareRequest;
import com.vsl.social.dto.ShareResponse;

public interface ShareService {

    /**
     * Chia sẻ một bài công khai: COPY_URL (lấy link) hoặc PROFILE (lên trang của mình, hoặc của một người bạn -
     * hai người phải theo dõi nhau). Trả tổng lượt chia sẻ mới + đường dẫn bài.
     */
    ShareResponse share(Long blogId, Long userId, ShareRequest request);
}
