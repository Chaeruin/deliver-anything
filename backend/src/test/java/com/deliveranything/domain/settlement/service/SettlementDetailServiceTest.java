package com.deliveranything.domain.settlement.service;

import com.deliveranything.domain.settlement.dto.SettlementDetailResponse;
import com.deliveranything.domain.settlement.dto.UnsettledResponse;
import com.deliveranything.domain.settlement.entity.SettlementDetail;
import com.deliveranything.domain.settlement.enums.SettlementStatus;
import com.deliveranything.domain.settlement.repository.SettlementDetailRepository;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementDetailServiceTest {

    @Mock
    private SettlementDetailRepository settlementDetailRepository;

    @InjectMocks
    private SettlementDetailService settlementDetailService;

    private final Long TEST_ORDER_ID = 1L;
    private final Long TEST_PROFILE_ID = 10L;
    private final Long TEST_STORE_PRICE = 100000L;
    private final Long TEST_DELIVERY_PRICE = 5000L;

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 Mock 초기화
    }

    @Test
    @DisplayName("판매자 정산 생성 성공")
    void createSellerSettlement_success() {
        // Given
        ArgumentCaptor<SettlementDetail> captor = ArgumentCaptor.forClass(SettlementDetail.class);

        // When
        settlementDetailService.createSellerSettlement(TEST_ORDER_ID, TEST_PROFILE_ID, TEST_STORE_PRICE);

        // Then
        verify(settlementDetailRepository, times(1)).save(captor.capture());
        SettlementDetail savedDetail = captor.getValue();
        assertThat(savedDetail.getOrderId()).isEqualTo(TEST_ORDER_ID);
        assertThat(savedDetail.getTargetId()).isEqualTo(TEST_PROFILE_ID);
        assertThat(savedDetail.getTargetAmount()).isEqualTo(TEST_STORE_PRICE - (long) (TEST_STORE_PRICE * 0.08));
        assertThat(savedDetail.getPlatformFee()).isEqualTo((long) (TEST_STORE_PRICE * 0.08));
        assertThat(savedDetail.getStatus()).isEqualTo(SettlementStatus.PENDING);
    }

    @Test
    @DisplayName("라이더 정산 생성 성공")
    void createRiderSettlement_success() {
        // Given
        ArgumentCaptor<SettlementDetail> captor = ArgumentCaptor.forClass(SettlementDetail.class);

        // When
        settlementDetailService.createRiderSettlement(TEST_ORDER_ID, TEST_PROFILE_ID, TEST_DELIVERY_PRICE);

        // Then
        verify(settlementDetailRepository, times(1)).save(captor.capture());
        SettlementDetail savedDetail = captor.getValue();
        assertThat(savedDetail.getOrderId()).isEqualTo(TEST_ORDER_ID);
        assertThat(savedDetail.getTargetId()).isEqualTo(TEST_PROFILE_ID);
        assertThat(savedDetail.getTargetAmount()).isEqualTo(TEST_DELIVERY_PRICE);
        assertThat(savedDetail.getPlatformFee()).isEqualTo(0L);
        assertThat(savedDetail.getStatus()).isEqualTo(SettlementStatus.PENDING);
    }

    @Test
    @DisplayName("라이더 정산 상세 조회 성공")
    void getRiderSettlementDetail_success() {
        // Given
        SettlementDetail mockDetail = mock(SettlementDetail.class);
        when(mockDetail.getOrderId()).thenReturn(TEST_ORDER_ID);
        when(mockDetail.getStatus()).thenReturn(SettlementStatus.COMPLETED);

        when(settlementDetailRepository.findByOrderIdAndTargetId(TEST_ORDER_ID, TEST_PROFILE_ID))
                .thenReturn(Optional.of(mockDetail));

        // When
        SettlementDetailResponse result = settlementDetailService.getRiderSettlementDetail(TEST_ORDER_ID, TEST_PROFILE_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.orderId()).isEqualTo(TEST_ORDER_ID);
        assertThat(result.settlementStatus()).isEqualTo(SettlementStatus.COMPLETED);
        verify(settlementDetailRepository, times(1)).findByOrderIdAndTargetId(TEST_ORDER_ID, TEST_PROFILE_ID);
    }

    @Test
    @DisplayName("라이더 정산 상세 조회 실패 - 정산 정보 없음")
    void getRiderSettlementDetail_notFound_throwsException() {
        // Given
        when(settlementDetailRepository.findByOrderIdAndTargetId(TEST_ORDER_ID, TEST_PROFILE_ID))
                .thenReturn(Optional.empty());

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () ->
                settlementDetailService.getRiderSettlementDetail(TEST_ORDER_ID, TEST_PROFILE_ID));
        assertThat(exception.getCode()).isEqualTo(ErrorCode.SETTLEMENT_DETAIL_NOT_FOUND.getCode());
        verify(settlementDetailRepository, times(1)).findByOrderIdAndTargetId(TEST_ORDER_ID, TEST_PROFILE_ID);
    }

    @Test
    @DisplayName("미정산 정보 조회 성공")
    void getUnsettledDetail_success() {
        // Given
        SettlementDetail mockDetail1 = mock(SettlementDetail.class);
        when(mockDetail1.getTargetAmount()).thenReturn(10000L);
        SettlementDetail mockDetail2 = mock(SettlementDetail.class);
        when(mockDetail2.getTargetAmount()).thenReturn(5000L);
        List<SettlementDetail> mockDetails = Arrays.asList(mockDetail1, mockDetail2);

        when(settlementDetailRepository.findAllUnsettledDetails(eq(TEST_PROFILE_ID), eq(SettlementStatus.PENDING),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockDetails);

        // When
        UnsettledResponse result = settlementDetailService.getUnsettledDetail(TEST_PROFILE_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.scheduledSettleAmount()).isEqualTo(15000L); // 10000 + 5000
        assertThat(result.scheduledTransactionCount()).isEqualTo(2);
        verify(settlementDetailRepository, times(1)).findAllUnsettledDetails(eq(TEST_PROFILE_ID), eq(SettlementStatus.PENDING),
                any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("어제 미정산 정보 조회 성공")
    void getYesterdayUnsettledDetails_success() {
        // Given
        List<SettlementDetail> mockDetails = Collections.singletonList(mock(SettlementDetail.class));
        when(settlementDetailRepository.findAllByStatusAndDateTime(eq(SettlementStatus.PENDING),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockDetails);

        // When
        List<SettlementDetail> result = settlementDetailService.getYesterdayUnsettledDetails();

        // Then
        assertThat(result).hasSize(1);
        verify(settlementDetailRepository, times(1)).findAllByStatusAndDateTime(eq(SettlementStatus.PENDING),
                any(LocalDateTime.class), any(LocalDateTime.class));
    }
}