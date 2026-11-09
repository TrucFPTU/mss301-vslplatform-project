package com.vsl.identity.service.impl;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OtpServiceImplTest {

    private static final String EMAIL = "a@b.vn";
    private final AtomicLong now = new AtomicLong(1_000_000);
    private final OtpServiceImpl service = new OtpServiceImpl(now::get);

    private static String wrong(String otp) {
        return otp.equals("000000") ? "111111" : "000000";
    }

    @Test
    @DisplayName("Mã đúng và còn hạn -> hợp lệ; mã 6 chữ số")
    void verify_Correct() {
        String otp = service.generateAndStoreOtp(EMAIL);
        assertThat(otp).matches("\\d{6}");
        assertThat(service.verifyOtp(EMAIL, otp)).isTrue();
    }

    @Test
    @DisplayName("Hết 5 phút -> không hợp lệ")
    void verify_Expired() {
        String otp = service.generateAndStoreOtp(EMAIL);
        now.addAndGet(OtpServiceImpl.OTP_EXPIRATION_TIME_MS + 1);
        assertThat(service.verifyOtp(EMAIL, otp)).isFalse();
    }

    @Test
    @DisplayName("Nhập sai đủ 5 lần -> hủy mã, báo OTP_ATTEMPTS_EXCEEDED; mã đúng sau đó cũng không dùng được")
    void verify_TooManyWrongAttempts() {
        String otp = service.generateAndStoreOtp(EMAIL);
        for (int i = 1; i < OtpServiceImpl.MAX_WRONG_ATTEMPTS; i++) {
            assertThat(service.verifyOtp(EMAIL, wrong(otp))).isFalse();
        }
        assertThatThrownBy(() -> service.verifyOtp(EMAIL, wrong(otp)))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.OTP_ATTEMPTS_EXCEEDED));
        assertThat(service.verifyOtp(EMAIL, otp)).isFalse();
    }

    @Test
    @DisplayName("Sai vài lần (chưa tới giới hạn) rồi nhập đúng vẫn được")
    void verify_CorrectAfterFewMistakes() {
        String otp = service.generateAndStoreOtp(EMAIL);
        service.verifyOtp(EMAIL, wrong(otp));
        service.verifyOtp(EMAIL, null);
        assertThat(service.verifyOtp(EMAIL, otp)).isTrue();
    }

    @Test
    @DisplayName("Gửi lại trong vòng 60 giây -> OTP_RESEND_TOO_SOON; sau 60 giây được cấp mã mới, mã cũ hết dùng")
    void resend_Cooldown() {
        String first = service.generateAndStoreOtp(EMAIL);
        now.addAndGet(OtpServiceImpl.RESEND_COOLDOWN_MS - 1);
        assertThatThrownBy(() -> service.generateAndStoreOtp(EMAIL))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.OTP_RESEND_TOO_SOON));
        now.addAndGet(1);
        String second = service.generateAndStoreOtp(EMAIL);
        if (!second.equals(first)) {
            assertThat(service.verifyOtp(EMAIL, first)).isFalse();
        }
        assertThat(service.verifyOtp(EMAIL, second)).isTrue();
    }

    @Test
    @DisplayName("Hủy mã thì gửi lại được ngay (dùng khi gửi email thất bại)")
    void clear_AllowsImmediateResend() {
        service.generateAndStoreOtp(EMAIL);
        service.clearOtp(EMAIL);
        assertThat(service.generateAndStoreOtp(EMAIL)).matches("\\d{6}");
    }
}
