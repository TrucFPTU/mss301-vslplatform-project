package com.vsl.identity.service;

import com.vsl.identity.dto.ChangePasswordRequest;
import com.vsl.identity.dto.UpdateProfileRequest;
import com.vsl.identity.dto.UserDto;

public interface UserService {

    UserDto getProfile(Long userId);

    UserDto updateProfile(Long userId, UpdateProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    UserDto getUserById(Long userId);
}
