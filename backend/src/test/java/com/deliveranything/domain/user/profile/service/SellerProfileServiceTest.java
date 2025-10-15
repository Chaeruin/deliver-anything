package com.deliveranything.domain.user.profile.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deliveranything.domain.user.profile.entity.SellerProfile;
import com.deliveranything.domain.user.profile.repository.SellerProfileRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SellerProfileService 단위 테스트")
class SellerProfileServiceTest {

  @Mock
  private SellerProfileRepository sellerProfileRepository;

  @InjectMocks
  private SellerProfileService sellerProfileService;

  @Nested
  @DisplayName("프로필 조회 테스트")
  class GetProfileTest {

    @Test
    @DisplayName("성공 - profileId로 프로필 조회")
    void getProfileByProfileId_success() {
      SellerProfile mockProfile = mock(SellerProfile.class);
      when(sellerProfileRepository.findByProfileId(10L)).thenReturn(Optional.of(mockProfile));

      SellerProfile result = sellerProfileService.getProfileByProfileId(10L);

      assertNotNull(result);
      assertEquals(mockProfile, result);
    }

    @Test
    @DisplayName("실패 - 프로필 없음")
    void getProfileByProfileId_not_found() {
      when(sellerProfileRepository.findByProfileId(10L)).thenReturn(Optional.empty());

      SellerProfile result = sellerProfileService.getProfileByProfileId(10L);

      assertNull(result);
    }
  }

  @Nested
  @DisplayName("프로필 수정 테스트")
  class UpdateProfileTest {

    @Test
    @DisplayName("성공 - 프로필 수정")
    void updateProfileByProfileId_success() {
      SellerProfile mockProfile = mock(SellerProfile.class);
      when(sellerProfileRepository.findByProfileId(10L)).thenReturn(Optional.of(mockProfile));
      when(sellerProfileRepository.save(mockProfile)).thenReturn(mockProfile);

      boolean result = sellerProfileService.updateProfileByProfileId(
          10L, "새닉네임", "http://new-image.url"
      );

      assertTrue(result);
      verify(mockProfile, times(1)).updateProfile("새닉네임", "http://new-image.url");
      verify(sellerProfileRepository, times(1)).save(mockProfile);
    }

    @Test
    @DisplayName("실패 - 프로필을 찾을 수 없음")
    void updateProfileByProfileId_fail_not_found() {
      when(sellerProfileRepository.findByProfileId(10L)).thenReturn(Optional.empty());

      boolean result = sellerProfileService.updateProfileByProfileId(
          10L, "새닉네임", "http://new-image.url"
      );

      assertFalse(result);
      verify(sellerProfileRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("사업자 정보 수정 테스트")
  class UpdateBusinessInfoTest {

    @Test
    @DisplayName("성공 - 사업자 정보 수정")
    void updateBusinessInfoByProfileId_success() {
      SellerProfile mockProfile = mock(SellerProfile.class);
      when(sellerProfileRepository.findByProfileId(10L)).thenReturn(Optional.of(mockProfile));
      when(sellerProfileRepository.save(mockProfile)).thenReturn(mockProfile);

      boolean result = sellerProfileService.updateBusinessInfoByProfileId(
          10L, "새사업자명", "02-9876-5432"
      );

      assertTrue(result);
      verify(mockProfile, times(1)).updateBusinessInfo("새사업자명", "02-9876-5432");
      verify(sellerProfileRepository, times(1)).save(mockProfile);
    }

    @Test
    @DisplayName("실패 - 프로필을 찾을 수 없음")
    void updateBusinessInfoByProfileId_fail_not_found() {
      when(sellerProfileRepository.findByProfileId(10L)).thenReturn(Optional.empty());

      boolean result = sellerProfileService.updateBusinessInfoByProfileId(
          10L, "새사업자명", "02-9876-5432"
      );

      assertFalse(result);
      verify(sellerProfileRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("계좌 정보 수정 테스트")
  class UpdateBankInfoTest {

    @Test
    @DisplayName("성공 - 계좌 정보 수정")
    void updateBankInfoByProfileId_success() {
      SellerProfile mockProfile = mock(SellerProfile.class);
      when(sellerProfileRepository.findByProfileId(10L)).thenReturn(Optional.of(mockProfile));
      when(sellerProfileRepository.save(mockProfile)).thenReturn(mockProfile);

      boolean result = sellerProfileService.updateBankInfoByProfileId(
          10L, "국민은행", "123-456-789012", "김철수"
      );

      assertTrue(result);
      verify(mockProfile, times(1)).updateBankInfo("국민은행", "123-456-789012", "김철수");
      verify(sellerProfileRepository, times(1)).save(mockProfile);
    }

    @Test
    @DisplayName("실패 - 프로필을 찾을 수 없음")
    void updateBankInfoByProfileId_fail_not_found() {
      when(sellerProfileRepository.findByProfileId(10L)).thenReturn(Optional.empty());

      boolean result = sellerProfileService.updateBankInfoByProfileId(
          10L, "국민은행", "123-456-789012", "김철수"
      );

      assertFalse(result);
      verify(sellerProfileRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("사업자등록번호 관련 테스트")
  class BusinessCertificateTest {

    @Test
    @DisplayName("성공 - 사업자등록번호 중복 확인")
    void existsByBusinessCertificateNumber_true() {
      when(sellerProfileRepository.existsByBusinessCertificateNumber("123-45-67890"))
          .thenReturn(true);

      boolean result = sellerProfileService.existsByBusinessCertificateNumber("123-45-67890");

      assertTrue(result);
    }

    @Test
    @DisplayName("성공 - 사업자등록번호로 프로필 조회")
    void getProfileByBusinessCertificateNumber_success() {
      SellerProfile mockProfile = mock(SellerProfile.class);
      when(sellerProfileRepository.findByBusinessCertificateNumber("123-45-67890"))
          .thenReturn(Optional.of(mockProfile));

      SellerProfile result = sellerProfileService.getProfileByBusinessCertificateNumber(
          "123-45-67890");

      assertNotNull(result);
      assertEquals(mockProfile, result);
    }
  }
}