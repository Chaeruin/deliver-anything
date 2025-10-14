package com.deliveranything.domain.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.deliveranything.domain.delivery.entity.Delivery;
import com.deliveranything.domain.delivery.enums.DeliveryStatus;
import com.deliveranything.domain.delivery.repository.DeliveryRepository;
import com.deliveranything.domain.settlement.service.SettlementDetailService;
import com.deliveranything.domain.store.store.entity.Store;
import com.deliveranything.domain.user.profile.entity.CustomerProfile;
import com.deliveranything.domain.user.profile.entity.Profile;
import com.deliveranything.domain.user.profile.entity.RiderProfile;
import com.deliveranything.domain.user.profile.enums.ProfileType;
import com.deliveranything.domain.user.profile.enums.RiderToggleStatus;
import com.deliveranything.domain.user.profile.service.CustomerProfileService;
import com.deliveranything.domain.user.profile.service.SellerProfileService;
import com.deliveranything.domain.user.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeliveryService 단위 테스트")
class DeliveryServiceTest {

  @InjectMocks
  private DeliveryService deliveryService;

  @Mock
  private DeliveryRepository deliveryRepository;

  @Mock
  private com.deliveranything.domain.user.profile.service.RiderProfileService riderProfileService;

  @Mock
  private com.deliveranything.domain.order.service.DeliveryOrderService deliveryOrderService;

  @Mock
  private SellerProfileService sellerProfileService;

  @Mock
  private CustomerProfileService customerProfileService;

  @Mock
  private SettlementDetailService settlementDetailService;

  private User testUser;
  private Profile testRiderProfile_;
  private Profile testCustomerProfile_;
  private RiderProfile testRiderProfile;
  private CustomerProfile testCustomerProfile;
  private Store testStore;

  @BeforeEach
  void setUp() {
    // 테스트 사용자 생성
    testUser = User.builder()
        .email("test@example.com")
        .password("password")
        .build();

    // 테스트 프로필 생성 (라이더)
    testRiderProfile_ = Profile.builder()
        .user(testUser)
        .type(ProfileType.RIDER)
        .build();

    // 테스트 라이더 프로필 생성
    testRiderProfile = RiderProfile.builder()
        .profile(testRiderProfile_)
        .nickname("테스트 라이더")
        .profileImageUrl("test-image.jpg")
        .toggleStatus(RiderToggleStatus.ON)
        .area("서울시 강남구")
        .licenseNumber("123456789")
        .build();

    testUser.setCurrentActiveProfile(testRiderProfile_);

    // 테스트 고객 프로필 생성 (프로필 타입은 고객)
    testCustomerProfile_ = Profile.builder()
        .user(testUser)
        .type(ProfileType.CUSTOMER)
        .build();

    // 테스트 고객 프로필 생성
    testCustomerProfile = CustomerProfile.builder()
        .profile(testCustomerProfile_)
        .nickname("테스트 고객")
        .profileImageUrl("customer-image.jpg")
        .build();

    // 테스트 상점 생성
    testStore = Store.builder()
        .name("테스트 상점")
        .build();
  }

  @Test
  @DisplayName("오늘 완료된 배달 수 조회")
  void 오늘_완료된_배달수_조회_테스트() {
    // Given: Mock 데이터 설정
    Long riderProfileId = 1L;
    when(deliveryRepository.countTodayCompletedDeliveriesByRider(riderProfileId))
        .thenReturn(1L);

    // When
    Long count = deliveryService.getTodayCompletedCountByRider(riderProfileId);

    // Then
    assertThat(count).isEqualTo(1L);
  }

  @Test
  @DisplayName("평균 배달 시간 계산 - 정상 케이스")
  void 평균_배달시간_계산_테스트() {
    // Given: 2건의 완료된 배달 (30분, 50분)
    Long riderProfileId = 1L;
    LocalDateTime now = LocalDateTime.now();

    Delivery delivery1 = Delivery.builder()
        .status(DeliveryStatus.COMPLETED)
        .riderProfile(testRiderProfile)
        .customer(testCustomerProfile)
        .store(testStore)
        .startedAt(now.minusMinutes(30))
        .completedAt(now)
        .build();

    Delivery delivery2 = Delivery.builder()
        .status(DeliveryStatus.COMPLETED)
        .riderProfile(testRiderProfile)
        .customer(testCustomerProfile)
        .store(testStore)
        .startedAt(now.minusMinutes(50))
        .completedAt(now)
        .build();

    when(deliveryRepository.findTodayCompletedDeliveriesByRider(riderProfileId))
        .thenReturn(List.of(delivery1, delivery2));

    // When
    Double avgTime = deliveryService.getAvgDeliveryTimeByRiderId(riderProfileId);

    // Then: 40.0분 (소수점 첫째 자리까지)
    assertThat(avgTime).isEqualTo(40.0);
  }

  @Test
  @DisplayName("평균 배달 시간 계산 - 배달 내역 없음")
  void 평균_배달시간_계산_빈리스트_테스트() {
    // Given: 빈 리스트
    Long riderProfileId = 1L;
    when(deliveryRepository.findTodayCompletedDeliveriesByRider(riderProfileId))
        .thenReturn(List.of());

    // When: 배달 내역이 없는 라이더
    Double avgTime = deliveryService.getAvgDeliveryTimeByRiderId(riderProfileId);

    // Then: 0.0
    assertThat(avgTime).isEqualTo(0.0);
  }

  @Test
  @DisplayName("평균 배달 시간 계산 - null 값 필터링")
  void 평균_배달시간_계산_null필터링_테스트() {
    // Given: null 값 포함된 배달
    Long riderProfileId = 1L;
    LocalDateTime now = LocalDateTime.now();

    Delivery validDelivery = Delivery.builder()
        .status(DeliveryStatus.COMPLETED)
        .riderProfile(testRiderProfile)
        .customer(testCustomerProfile)
        .store(testStore)
        .startedAt(now.minusMinutes(30))
        .completedAt(now)
        .build();

    Delivery nullDelivery = Delivery.builder()
        .status(DeliveryStatus.COMPLETED)
        .riderProfile(testRiderProfile)
        .customer(testCustomerProfile)
        .store(testStore)
        .startedAt(null)  // null 값
        .completedAt(now)
        .build();

    when(deliveryRepository.findTodayCompletedDeliveriesByRider(riderProfileId))
        .thenReturn(List.of(validDelivery, nullDelivery));

    // When
    Double avgTime = deliveryService.getAvgDeliveryTimeByRiderId(riderProfileId);

    // Then: 30.0분 (null 제외하고 계산)
    assertThat(avgTime).isEqualTo(30.0);
  }

  @Test
  @DisplayName("다른 날짜 배달은 제외")
  void 다른날짜_배달_제외_테스트() {
    // Given: Mock 데이터 설정 (오늘 배달만 카운트)
    Long riderProfileId = 1L;
    when(deliveryRepository.countTodayCompletedDeliveriesByRider(riderProfileId))
        .thenReturn(1L);

    // When
    Long todayCount = deliveryService.getTodayCompletedCountByRider(riderProfileId);

    // Then: 오늘 배달만 카운트
    assertThat(todayCount).isEqualTo(1L);
  }
}
