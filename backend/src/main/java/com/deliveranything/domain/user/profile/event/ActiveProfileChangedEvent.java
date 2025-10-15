package com.deliveranything.domain.user.profile.event;

public record ActiveProfileChangedEvent(
    Long oldProfileId,
    Long newProfileId,
    String deviceId
) {
}
