package com.deliveranything.domain.settlement.repository;

import com.deliveranything.domain.settlement.entity.SettlementDetail;
import com.deliveranything.domain.settlement.enums.SettlementStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SettlementDetailRepository extends JpaRepository<SettlementDetail, Long> {

  Optional<SettlementDetail> findByOrderIdAndTargetId(Long orderId, Long targetId);

  @Query("SELECT sd FROM SettlementDetail sd WHERE sd.status = :status "
      + "AND :startDateTime <= sd.createdAt AND sd.createdAt < :endDateTime")
  List<SettlementDetail> findAllByStatusAndDateTime(
      @Param("status") SettlementStatus status,
      @Param("startDateTime") LocalDateTime startDateTime,
      @Param("endDateTime") LocalDateTime endDateTime
  );

  @Query("""
      SELECT sd FROM SettlementDetail sd
      WHERE sd.targetId = :targetId
        AND sd.status = :status
        AND sd.createdAt >= :startDateTime
        AND sd.createdAt < :endDateTime
      """)
  List<SettlementDetail> findAllUnsettledDetails(
      Long targetId,
      SettlementStatus status,
      LocalDateTime startDateTime,
      LocalDateTime endDateTime
  );
}
