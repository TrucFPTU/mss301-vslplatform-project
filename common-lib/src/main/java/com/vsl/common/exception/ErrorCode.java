package com.vsl.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Danh mục mã lỗi dùng chung toàn hệ thống.
 * Quy ước code: {DOMAIN}_{4 số}. Mỗi service có thể bổ sung mã riêng theo cùng quy ước.
 */
@Getter
public enum ErrorCode {

    // ---- Chung ----
    INTERNAL_ERROR("SYS_0001", "Lỗi hệ thống", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_ERROR("SYS_0002", "Dữ liệu không hợp lệ", HttpStatus.BAD_REQUEST),
    NOT_FOUND("SYS_0003", "Không tìm thấy tài nguyên", HttpStatus.NOT_FOUND),
    FORBIDDEN("SYS_0004", "Không có quyền truy cập", HttpStatus.FORBIDDEN),
    METHOD_NOT_ALLOWED("SYS_0005", "Phương thức HTTP không được hỗ trợ", HttpStatus.METHOD_NOT_ALLOWED),
    PAYLOAD_TOO_LARGE("SYS_0006", "Dữ liệu tải lên vượt quá dung lượng cho phép", HttpStatus.PAYLOAD_TOO_LARGE),
    UNSUPPORTED_MEDIA_TYPE("SYS_0007", "Định dạng nội dung không được hỗ trợ", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    CONFLICT("SYS_0008", "Dữ liệu bị trùng hoặc xung đột", HttpStatus.CONFLICT),
    FILE_UPLOAD_ERROR("SYS_0009", "Lỗi khi tải file lên kho lưu trữ", HttpStatus.SERVICE_UNAVAILABLE),
    INVALID_FILE("SYS_0010", "File không hợp lệ", HttpStatus.BAD_REQUEST),
    SERVICE_UNAVAILABLE("SYS_0011", "Dịch vụ liên quan tạm thời không khả dụng", HttpStatus.SERVICE_UNAVAILABLE),

    // ---- Auth / Identity ----
    UNAUTHENTICATED("AUTH_1001", "Chưa xác thực", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("AUTH_1002", "Token không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS("AUTH_1003", "Sai thông tin đăng nhập", HttpStatus.UNAUTHORIZED),
    INVALID_OTP("AUTH_1004", "Mã OTP không đúng hoặc đã hết hạn", HttpStatus.BAD_REQUEST),

    // ---- Learning ----
    VOCABULARY_ALREADY_EXISTS("LEARNING_2001", "Từ vựng đã tồn tại", HttpStatus.CONFLICT),
    VOCABULARY_SUGGESTION_NOT_FOUND("LEARNING_2002", "Không tìm thấy đề xuất từ vựng", HttpStatus.NOT_FOUND),
    VOCABULARY_SUGGESTION_NOT_PENDING("LEARNING_2003", "Đề xuất đã được xem xét, không thể sửa/thu hồi", HttpStatus.CONFLICT),
    VOCABULARY_SUGGESTION_ALREADY_PENDING("LEARNING_2004", "Đã có đề xuất đang chờ duyệt cho từ này", HttpStatus.CONFLICT),
    VOCABULARY_EXPECTED_ID_TAKEN("LEARNING_2005", "expectedId (class AI) đã được gán cho một từ khác", HttpStatus.CONFLICT),
    CATEGORY_NAME_TAKEN("LEARNING_2006", "Tên danh mục đã tồn tại", HttpStatus.CONFLICT),

    // ---- AI ----
    AI_MODEL_NOT_LOADED("AI_3001", "Model AI chưa sẵn sàng", HttpStatus.SERVICE_UNAVAILABLE),
    AI_INFERENCE_ERROR("AI_3002", "Lỗi khi xử lý video", HttpStatus.INTERNAL_SERVER_ERROR),
    VIDEO_EMPTY("AI_3003", "Video rỗng", HttpStatus.BAD_REQUEST),
    VIDEO_INVALID_TYPE("AI_3004", "Định dạng video không hợp lệ", HttpStatus.BAD_REQUEST),
    VIDEO_CORRUPT("AI_3005", "Video bị hỏng hoặc định dạng không được hỗ trợ", HttpStatus.UNPROCESSABLE_ENTITY),
    VIDEO_NO_FRAMES("AI_3006", "Không thể đọc frame từ video", HttpStatus.UNPROCESSABLE_ENTITY),
    AI_SERVICE_BUSY("AI_3007", "Hệ thống AI đang quá tải, vui lòng thử lại sau", HttpStatus.SERVICE_UNAVAILABLE),
    MODERATION_ERROR("AI_3008", "Hệ thống kiểm duyệt nội dung đang gặp sự cố, vui lòng thử lại sau", HttpStatus.SERVICE_UNAVAILABLE),
    AI_EXTERNAL_ERROR("AI_3009", "Dịch vụ AI bên ngoài đang gặp sự cố, vui lòng thử lại sau", HttpStatus.SERVICE_UNAVAILABLE),

    // ---- Social ----
    BLOG_NOT_FOUND("SOCIAL_4001", "Không tìm thấy bài viết", HttpStatus.NOT_FOUND),
    COMMENT_NOT_FOUND("SOCIAL_4002", "Không tìm thấy bình luận", HttpStatus.NOT_FOUND),
    REPLY_NOT_FOUND("SOCIAL_4003", "Không tìm thấy trả lời", HttpStatus.NOT_FOUND),
    NOTIFICATION_NOT_FOUND("SOCIAL_4004", "Không tìm thấy thông báo", HttpStatus.NOT_FOUND),
    BLOG_HIDE_FORBIDDEN("SOCIAL_4005", "Chỉ ADMIN được ẩn bài viết", HttpStatus.FORBIDDEN),
    REPORT_OWN_BLOG("SOCIAL_4006", "Không thể báo cáo bài viết của chính mình", HttpStatus.BAD_REQUEST),
    REPORT_ALREADY_PENDING("SOCIAL_4007", "Bạn đã báo cáo bài viết này, đang chờ xử lý", HttpStatus.CONFLICT),
    CANNOT_FOLLOW_SELF("SOCIAL_4008", "Không thể tự theo dõi chính mình", HttpStatus.BAD_REQUEST),
    BLOG_REJECTED("SOCIAL_4009", "Bài viết không phù hợp với tiêu chuẩn cộng đồng", HttpStatus.UNPROCESSABLE_ENTITY),
    BLOG_CONTENT_ALREADY_REPORTED("SOCIAL_4010", "Nội dung này trùng với một bài viết đã bị tố cáo, không thể đăng", HttpStatus.CONFLICT),
    BLOG_DUPLICATE_CONTENT("SOCIAL_4011", "Nội dung trùng với một bài viết đã đăng", HttpStatus.CONFLICT),
    BLOG_DUPLICATE_TITLE("SOCIAL_4012", "Tiêu đề trùng với một bài viết đã đăng", HttpStatus.CONFLICT),
    BLOG_DUPLICATE_CONTENT_AI("SOCIAL_4013", "Nội dung trùng ý với một bài viết đã có", HttpStatus.CONFLICT),
    BLOG_UNDER_REPORT("SOCIAL_4014", "Bài viết đang bị tố cáo hoặc đã bị gỡ, không thể xóa", HttpStatus.CONFLICT),
    BLOG_HIDDEN_LOCKED("SOCIAL_4015", "Bài viết đã bị gỡ, không thể chỉnh sửa", HttpStatus.CONFLICT),
    COMMENT_REJECTED("SOCIAL_4016", "Bình luận không phù hợp với tiêu chuẩn cộng đồng", HttpStatus.UNPROCESSABLE_ENTITY),
    NOT_FRIEND("SOCIAL_4017", "Chỉ có thể chia sẻ lên trang cá nhân của bạn bè (hai người theo dõi nhau)", HttpStatus.FORBIDDEN),
    CANNOT_SHARE_TO_SELF("SOCIAL_4018", "Không thể chia sẻ lên trang cá nhân cho chính mình", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND("SOCIAL_4019", "Không tìm thấy người dùng", HttpStatus.NOT_FOUND);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }
}
