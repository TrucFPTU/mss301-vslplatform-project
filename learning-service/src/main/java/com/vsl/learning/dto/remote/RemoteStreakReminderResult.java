package com.vsl.learning.dto.remote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RemoteStreakReminderResult(int sent) {
}
