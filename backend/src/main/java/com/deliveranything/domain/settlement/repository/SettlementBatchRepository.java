package com.deliveranything.domain.settlement.repository;

import com.deliveranything.domain.settlement.dto.projection.SettlementProjection;
import com.deliveranything.domain.settlement.dto.projection.SettlementSummaryProjection;
import com.deliveranything.domain.settlement.entity.SettlementBatch;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SettlementBatchRepository extends JpaRepository<SettlementBatch, Long> {

  List<SettlementBatch> findAllByTargetId(Long targetId);

  // 주간별 집계 쿼리
  @Query(value = """
          SELECT new com.deliveranything.domain.settlement.dto.projection.SettlementProjection(
              SUM(s.targetTotalAmount),
              SUM(s.totalPlatformFee),
              SUM(s.settledAmount),
              SUM(s.transactionCount),
              MIN(s.settlementDate),
              MAX(s.settlementDate)
          )
          FROM SettlementBatch s
          WHERE s.targetId = :targetId
          GROUP BY YEAR(s.settlementDate), WEEK(s.settlementDate)
          ORDER BY MIN(s.settlementDate) DESC
      """)
  List<SettlementProjection> findWeeklySettlementsByTargetId(@Param("targetId") Long targetId);


  // 월별 집계 쿼리
  @Query(value = """
          SELECT new com.deliveranything.domain.settlement.dto.projection.SettlementProjection(
              SUM(s.targetTotalAmount),
              SUM(s.totalPlatformFee),
              SUM(s.settledAmount),
              SUM(s.transactionCount),
              MIN(s.settlementDate),
              MAX(s.settlementDate)
          )
          FROM SettlementBatch s
          WHERE s.targetId = :targetId
          GROUP BY FUNCTION('YEAR', s.settlementDate), FUNCTION('MONTH', s.settlementDate)
          ORDER BY MIN(s.settlementDate) DESC
      """)
  List<SettlementProjection> findMonthlySettlementsByTargetId(@Param("targetId") Long targetId);

  // 기간 정산
  @Query(value = """
          SELECT new com.deliveranything.domain.settlement.dto.projection.SettlementProjection(
              COALESCE(SUM(s.targetTotalAmount), 0L),
              COALESCE(SUM(s.totalPlatformFee), 0L),
              COALESCE(SUM(s.settledAmount), 0L),
              COALESCE(SUM(s.transactionCount), 0L),
              null,
              null
          )
          FROM SettlementBatch s
          WHERE s.targetId = :targetId
            AND s.settlementDate BETWEEN :startDate AND :endDate
      """)
  SettlementProjection findSettlementByTargetIdAndPeriod(
      @Param("targetId") Long targetId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate
  );

  // 요약 카드에 필요한 쿼리
  @Query(value = """
          SELECT new com.deliveranything.domain.settlement.dto.projection.SettlementSummaryProjection(
              CAST(COALESCE(SUM(s.transactionCount), 0) AS long),
              CAST(COALESCE(SUM(CASE
                  WHEN YEAR(s.settlementDate) = YEAR(CURRENT_DATE) AND WEEK(s.settlementDate) = WEEK(CURRENT_DATE)
                  THEN s.transactionCount
                  ELSE 0
              END), 0) AS long),
              CAST(COALESCE(SUM(CASE
                  WHEN YEAR(s.settlementDate) = YEAR(CURRENT_DATE) AND WEEK(s.settlementDate) = WEEK(CURRENT_DATE)
                  THEN s.settledAmount
                  ELSE 0
              END), 0) AS long),
              CAST(COALESCE(SUM(CASE
                  WHEN YEAR(s.settlementDate) = YEAR(CURRENT_DATE) AND MONTH(s.settlementDate) = MONTH(CURRENT_DATE)
                  THEN s.settledAmount
                  ELSE 0
              END), 0) AS long),
              CAST(COALESCE(SUM(s.settledAmount), 0) AS long)
          )
          FROM SettlementBatch s
          WHERE s.targetId = :targetId
      """)
  SettlementSummaryProjection findSettlementSummaryByTargetId(@Param("targetId") Long targetId);
}
