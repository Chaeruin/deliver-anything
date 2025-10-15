package com.deliveranything.domain.delivery.event.dto;

import com.deliveranything.domain.order.event.OrderAcceptedEvent;

public record OrderAssignFailedEvent(
    OrderAcceptedEvent order
) {

}
