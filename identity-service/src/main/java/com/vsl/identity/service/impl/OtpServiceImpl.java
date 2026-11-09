package com.vsl.identity.service.impl;

import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import com.vsl.identity.service.OtpService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Lưu OTP trong bộ nhớ (ConcurrentHashMap), hết hạn sau 5 phút.
 * Chống dò mã: mỗi mã chỉ được nhập sai {@value #MAX_WRONG_ATTEMPTS} lần, quá thì hủy mã.
 * Chống spam email: cùng một email phải chờ {@value #RESEND_COOLDOWN_MS} ms mới được cấp mã mới.
 */
@Service
public class OtpServiceImpl implements OtpService {

    static final long OTP_EXPIRATION_TIME_MS = 5 * 60 * 1000; // 5 phút
    static final int MAX_WRONG_ATTEMPTS = 5;
    static final long RESEND_COOLDOWN_MS = 60 * 1000; // FE đếm ngược 90 giây trước khi cho gửi lại

    private final Map<String, OtpDetails> otpCache = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private final LongSupplier clock;

    public OtpServiceImpl() {
        this(System::currentTimeMillis);
    }

    OtpServiceImpl(LongSupplier clock) {
        this.clock = clock;
    }

    @Override
    public String generateAndStoreOtp(String email) {
        long now = clock.getAsLong();
        OtpDetails previous = otpCache.get(email);
        if (previous != null && now - previous.issuedAt() < RESEND_COOLDOWN_MS) {
            throw new AppException(ErrorCode.OTP_RESEND_TOO_SOON);
        }
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        otpCache.put(email, new OtpDetails(otp, now, now + OTP_EXPIRATION_TIME_MS, 0));
        return otp;
    }

    @Override
    public boolean verifyOtp(String email, String otp) {
        OtpDetails details = otpCache.get(email);
        if (details == null) {
            return false;
        }
        if (clock.getAsLong() > details.expiryTime()) {
            otpCache.remove(email);
            return false;
        }
        if (otp != null && MessageDigest.isEqual(details.otp().getBytes(StandardCharsets.UTF_8),
                otp.getBytes(StandardCharsets.UTF_8))) {
            return true;
        }
        int wrong = details.wrongAttempts() + 1;
        if (wrong >= MAX_WRONG_ATTEMPTS) {
            otpCache.remove(email);
            throw new AppException(ErrorCode.OTP_ATTEMPTS_EXCEEDED);
        }
        otpCache.put(email, new OtpDetails(details.otp(), details.issuedAt(), details.expiryTime(), wrong));
        return false;
    }

    @Override
    public void clearOtp(String email) {
        otpCache.remove(email);
    }

    private record OtpDetails(String otp, long issuedAt, long expiryTime, int wrongAttempts) {
    }
}
