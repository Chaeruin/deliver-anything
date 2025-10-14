package com.deliveranything.domain.settlement.service;

import com.deliveranything.domain.settlement.dto.SettlementResponse;
import com.deliveranything.domain.settlement.dto.SummaryResponse;
import com.deliveranything.domain.settlement.dto.projection.SettlementProjection;
import com.deliveranything.domain.settlement.dto.projection.SettlementSummaryProjection;
import com.deliveranything.domain.settlement.entity.SettlementBatch;
import com.deliveranything.domain.settlement.entity.SettlementDetail;
import com.deliveranything.domain.settlement.repository.SettlementBatchRepository;
import com.deliveranything.domain.settlement.service.dto.SettlementSummary;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class SettlementBatchService {

  private final SettlementDetailService settlementDetailService;

  private final SettlementBatchRepository settlementBatchRepository;

  @Transactional(readOnly = true)
  public List<SettlementResponse> getSettlementsByDay(Long targetId) {
    return settlementBatchRepository.findAllByTargetId(targetId).stream()
        .map(SettlementResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<SettlementResponse> getSettlementsByWeek(Long targetId) {
    return settlementBatchRepository.findWeeklySettlementsByTargetId(targetId).stream()
        .map(SettlementResponse::fromProjection)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<SettlementResponse> getSettlementsByMonth(Long targetId) {
    return settlementBatchRepository.findMonthlySettlementsByTargetId(targetId).stream()
        .map(SettlementResponse::fromProjection)
        .toList();
  }

  @Transactional(readOnly = true)
  public SettlementResponse getSettlementByPeriod(
      Long targetId,
      LocalDate startDate,
      LocalDate endDate
  ) {
    SettlementProjection settlementProjection = settlementBatchRepository
        .findSettlementByTargetIdAndPeriod(targetId, startDate, endDate);
    return SettlementResponse.fromProjectionAndPeriod(settlementProjection, startDate, endDate);
  }

  @Transactional(readOnly = true)
  public SummaryResponse getSettlementSummary(Long targetId) {
    return SummaryResponse.fromSettledAndUnsettled(getSettlementBatchSummary(targetId),
        settlementDetailService.getUnsettledDetail(targetId));
  }

  // "초 분 시 일 월 요일"
  // "0 0 0 * * *" 매일 0시 0분 0초에 실행
  @Scheduled(cron = "0 0 0 * * *")
  @Transactional
  public void processDailySettlements() {
    List<SettlementDetail> settlementDetails = settlementDetailService.getYesterdayUnsettledDetails();

    if (settlementDetails.isEmpty()) {
      return;
    }

    // 데이터 집계: 판매자/라이더 회원 별로 그룹핑하여 금액 합산
    Map<Long, SettlementSummary> summaryMap = settlementDetails.stream()
        .collect(Collectors.groupingBy(
            SettlementDetail::getTargetId,
            Collectors.collectingAndThen(
                Collectors.toList(),
                list -> new SettlementSummary(
                    list.stream().mapToLong(SettlementDetail::getTargetAmount).sum(),
                    list.stream().mapToLong(SettlementDetail::getPlatformFee).sum(),
                    list.size()
                )
            )
        ));

    // 그룹 정산 생성 및 각 정산 대상 상태 업데이트
    summaryMap.forEach((targetId, summary) -> {
      SettlementBatch batch = SettlementBatch.builder()
          .settlementDate(LocalDate.now().minusDays(1))
          .targetId(targetId)
          .targetTotalAmount(summary.totalTargetAmount())
          .totalPlatformFee(summary.totalPlatformFee())
          .settledAmount(summary.totalTargetAmount() - summary.totalPlatformFee())
          .transactionCount(summary.transactionCount())
          .build();

      SettlementBatch savedBatch = settlementBatchRepository.save(batch);

      settlementDetails.stream()
          .filter(d -> d.getTargetId().equals(targetId))
          .forEach(detail -> detail.process(savedBatch.getId()));
    });
  }

  // 요약 카드에 필요한 정산된 데이터 조회
  private SettlementSummaryProjection getSettlementBatchSummary(Long targetId) {
    return settlementBatchRepository.findSettlementSummaryByTargetId(targetId);
  }
}
