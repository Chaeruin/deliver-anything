package com.deliveranything.domain.settlement.service;

import com.deliveranything.domain.settlement.dto.SettlementResponse;
import com.deliveranything.domain.settlement.dto.SummaryResponse;
import com.deliveranything.domain.settlement.dto.UnsettledResponse;
import com.deliveranything.domain.settlement.dto.projection.SettlementProjection;
import com.deliveranything.domain.settlement.dto.projection.SettlementSummaryProjection;
import com.deliveranything.domain.settlement.entity.SettlementBatch;
import com.deliveranything.domain.settlement.entity.SettlementDetail;
import com.deliveranything.domain.settlement.repository.SettlementBatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementBatchServiceTest {

    @Mock
    private SettlementBatchRepository settlementBatchRepository;
    @Mock
    private SettlementDetailService settlementDetailService;

    @InjectMocks
    private SettlementBatchService settlementBatchService;

    private final Long TEST_TARGET_ID = 1L;
    private final LocalDate TEST_DATE = LocalDate.of(2023, 10, 26);

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 Mock 초기화
    }

    @Test
    @DisplayName("일별 정산 처리 성공 - 정산 대상이 없는 경우")
    void processDailySettlements_noDetails_success() {
        // Given
        when(settlementDetailService.getYesterdayUnsettledDetails()).thenReturn(Collections.emptyList());

        // When
        settlementBatchService.processDailySettlements();

        // Then
        verify(settlementDetailService, times(1)).getYesterdayUnsettledDetails();
        verifyNoInteractions(settlementBatchRepository); // 정산 대상이 없으므로 repository와 상호작용 없음
    }

    @Test
    @DisplayName("일별 정산 처리 성공 - 정산 대상이 있는 경우")
    void processDailySettlements_withDetails_success() {
        // Given
        SettlementDetail detail1 = mock(SettlementDetail.class);
        when(detail1.getTargetId()).thenReturn(TEST_TARGET_ID);
        when(detail1.getTargetAmount()).thenReturn(10000L);
        when(detail1.getPlatformFee()).thenReturn(1000L);

        SettlementDetail detail2 = mock(SettlementDetail.class);
        when(detail2.getTargetId()).thenReturn(TEST_TARGET_ID);
        when(detail2.getTargetAmount()).thenReturn(20000L);
        when(detail2.getPlatformFee()).thenReturn(2000L);

        List<SettlementDetail> settlementDetails = Arrays.asList(detail1, detail2);
        when(settlementDetailService.getYesterdayUnsettledDetails()).thenReturn(settlementDetails);

        SettlementBatch savedBatch = mock(SettlementBatch.class);
        when(savedBatch.getId()).thenReturn(1L);
        when(settlementBatchRepository.save(any(SettlementBatch.class))).thenReturn(savedBatch);

        // When
        settlementBatchService.processDailySettlements();

        // Then
        verify(settlementDetailService, times(1)).getYesterdayUnsettledDetails();
        verify(settlementBatchRepository, times(1)).save(any(SettlementBatch.class));
        verify(detail1, times(1)).process(savedBatch.getId());
        verify(detail2, times(1)).process(savedBatch.getId());
    }

    @Test
    @DisplayName("일별 정산 조회 성공")
    void getSettlementsByDay_success() {
        // Given
        SettlementBatch mockBatch = SettlementBatch.builder()
                .settlementDate(TEST_DATE)
                .targetId(TEST_TARGET_ID)
                .targetTotalAmount(10000L)
                .totalPlatformFee(1000L)
                .settledAmount(9000L)
                .transactionCount(1)
                .build();
        when(settlementBatchRepository.findAllByTargetId(TEST_TARGET_ID)).thenReturn(Collections.singletonList(mockBatch));

        // When
        List<SettlementResponse> result = settlementBatchService.getSettlementsByDay(TEST_TARGET_ID);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).startDate()).isEqualTo(TEST_DATE);
        verify(settlementBatchRepository, times(1)).findAllByTargetId(TEST_TARGET_ID);
    }

    @Test
    @DisplayName("주별 정산 조회 성공")
    void getSettlementsByWeek_success() {
        // Given
        SettlementProjection mockProjection = mock(SettlementProjection.class);
        when(mockProjection.minDate()).thenReturn(TEST_DATE);
        when(mockProjection.targetTotalAmount()).thenReturn(50000L);
        when(mockProjection.totalPlatformFee()).thenReturn(5000L);
        when(mockProjection.settledAmount()).thenReturn(45000L);
        when(mockProjection.transactionCount()).thenReturn(5L);

        when(settlementBatchRepository.findWeeklySettlementsByTargetId(TEST_TARGET_ID))
                .thenReturn(Collections.singletonList(mockProjection));

        // When
        List<SettlementResponse> result = settlementBatchService.getSettlementsByWeek(TEST_TARGET_ID);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).startDate()).isEqualTo(TEST_DATE);
        verify(settlementBatchRepository, times(1)).findWeeklySettlementsByTargetId(TEST_TARGET_ID);
    }

    @Test
    @DisplayName("월별 정산 조회 성공")
    void getSettlementsByMonth_success() {
        // Given
        SettlementProjection mockProjection = mock(SettlementProjection.class);
        when(mockProjection.minDate()).thenReturn(TEST_DATE);
        when(mockProjection.targetTotalAmount()).thenReturn(100000L);
        when(mockProjection.totalPlatformFee()).thenReturn(10000L);
        when(mockProjection.settledAmount()).thenReturn(90000L);
        when(mockProjection.transactionCount()).thenReturn(10L);

        when(settlementBatchRepository.findMonthlySettlementsByTargetId(TEST_TARGET_ID))
                .thenReturn(Collections.singletonList(mockProjection));

        // When
        List<SettlementResponse> result = settlementBatchService.getSettlementsByMonth(TEST_TARGET_ID);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).startDate()).isEqualTo(TEST_DATE);
        verify(settlementBatchRepository, times(1)).findMonthlySettlementsByTargetId(TEST_TARGET_ID);
    }

    @Test
    @DisplayName("기간별 정산 조회 성공")
    void getSettlementByPeriod_success() {
        // Given
        LocalDate startDate = LocalDate.of(2023, 10, 1);
        LocalDate endDate = LocalDate.of(2023, 10, 31);

        SettlementProjection mockProjection = mock(SettlementProjection.class);
        when(mockProjection.targetTotalAmount()).thenReturn(300000L);
        when(mockProjection.totalPlatformFee()).thenReturn(30000L);
        when(mockProjection.settledAmount()).thenReturn(270000L);
        when(mockProjection.transactionCount()).thenReturn(30L);

        when(settlementBatchRepository.findSettlementByTargetIdAndPeriod(TEST_TARGET_ID, startDate, endDate))
                .thenReturn(mockProjection);

        // When
        SettlementResponse result = settlementBatchService.getSettlementByPeriod(TEST_TARGET_ID, startDate, endDate);

        // Then
        assertThat(result.startDate()).isEqualTo(startDate);
        assertThat(result.endDate()).isEqualTo(endDate);
        assertThat(result.settledAmount()).isEqualTo(270000L);
        verify(settlementBatchRepository, times(1)).findSettlementByTargetIdAndPeriod(TEST_TARGET_ID, startDate, endDate);
    }

    @Test
    @DisplayName("정산 요약 조회 성공")
    void getSettlementSummary_success() {
        // Given
        SettlementSummaryProjection mockBatchSummary = mock(SettlementSummaryProjection.class);
        when(mockBatchSummary.totalSettledAmount()).thenReturn(100000L);

        UnsettledResponse mockUnsettledResponse = mock(UnsettledResponse.class);
        when(mockUnsettledResponse.scheduledTransactionCount()).thenReturn(1);
        when(mockUnsettledResponse.scheduledSettleAmount()).thenReturn(4500L);

        when(settlementBatchRepository.findSettlementSummaryByTargetId(TEST_TARGET_ID)).thenReturn(mockBatchSummary);
        when(settlementDetailService.getUnsettledDetail(TEST_TARGET_ID)).thenReturn(mockUnsettledResponse);

        // When
        SummaryResponse result = settlementBatchService.getSettlementSummary(TEST_TARGET_ID);

        // Then
        assertThat(result.totalSettledAmount()).isEqualTo(100000L);
        assertThat(result.scheduledSettleAmount()).isEqualTo(4500L);
        verify(settlementBatchRepository, times(1)).findSettlementSummaryByTargetId(TEST_TARGET_ID);
        verify(settlementDetailService, times(1)).getUnsettledDetail(TEST_TARGET_ID);
    }
}
