package com.vsl.identity.service.impl;

import com.vsl.identity.service.OtpService;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lưu OTP trong bộ nhớ (ConcurrentHashMap), hết hạn sau 5 phút.
 * Đủ cho phạm vi đồ án; nếu scale nhiều instance identity-service thì nên chuyển sang Redis.
 */
@Service
public class OtpServiceImpl implements OtpService {

    private static final long OTP_EXPIRATION_TIME_MS = 5 * 60 * 1000; // 5 phút

    private record OtpDetails(String otp, long expiryTime) {
    }

    private final Map<String, OtpDetails> otpCache = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateAndStoreOtp(String email) {
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        long expiryTime = System.currentTimeMillis() + OTP_EXPIRATION_TIME_MS;
        otpCache.put(email, new OtpDetails(otp, expiryTime));
        return otp;
    }

    @Override
    public boolean verifyOtp(String email, String otp) {
        OtpDetails details = otpCache.get(email);
        if (details == null) {
            return false;
        }
        if (System.currentTimeMillis() > details.expiryTime()) {
            otpCache.remove(email);
            return false;
        }
        return details.otp().equals(otp);
    }

    @Override
    public void clearOtp(String email) {
        otpCache.remove(email);
    }
}
