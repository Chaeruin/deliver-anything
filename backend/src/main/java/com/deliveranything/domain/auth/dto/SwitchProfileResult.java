package com.deliveranything.domain.auth.dto;

import com.deliveranything.domain.user.profile.dto.SwitchProfileResponse;
import lombok.Builder;

@Builder
public record SwitchProfileResult(
    SwitchProfileResponse switchProfileResponse,
    String accessToken
) {
}
