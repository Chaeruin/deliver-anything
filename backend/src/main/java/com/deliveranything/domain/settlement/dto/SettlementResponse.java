package com.deliveranything.domain.settlement.dto;

import com.deliveranything.domain.settlement.dto.projection.SettlementProjection;
import com.deliveranything.domain.settlement.entity.SettlementBatch;
import java.time.LocalDate;

public record SettlementResponse(
    Long totalAmount,
    Long totalPlatformFee,
    Long settledAmount,
    Integer transactionCount,
    LocalDate startDate,
    LocalDate endDate
) {

  public static SettlementResponse from(SettlementBatch settlement) {
    return new SettlementResponse(
        settlement.getTargetTotalAmount(),
        settlement.getTotalPlatformFee(),
        settlement.getSettledAmount(),
        settlement.getTransactionCount(),
        settlement.getSettlementDate(),
        settlement.getSettlementDate()
    );
  }

  public static SettlementResponse fromProjection(SettlementProjection sp) {
    return new SettlementResponse(
        sp.targetTotalAmount(),
        sp.totalPlatformFee(),
        sp.settledAmount(),
        sp.transactionCount().intValue(),
        sp.minDate(),
        sp.maxDate()
    );
  }

  public static SettlementResponse fromProjectionAndPeriod(
      SettlementProjection sp,
      LocalDate startDate,
      LocalDate endDate
  ) {
    return new SettlementResponse(
        sp.targetTotalAmount(),
        sp.totalPlatformFee(),
        sp.settledAmount(),
        sp.transactionCount().intValue(),
        startDate,
        endDate
    );
  }
}
