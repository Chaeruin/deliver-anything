package com.deliveranything.domain.user.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deliveranything.domain.user.user.entity.User;
import com.deliveranything.domain.user.user.repository.UserRepository;
import com.deliveranything.global.exception.CustomException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트")
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private UserService userService;

  @Nested
  @DisplayName("사용자 조회 테스트")
  class FindUserTest {

    @Test
    @DisplayName("성공 - ID로 사용자 조회")
    void findById_success() {
      User mockUser = mock(User.class);
      when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

      User result = userService.findById(1L);

      assertNotNull(result);
      assertEquals(mockUser, result);
    }

    @Test
    @DisplayName("실패 - 사용자를 찾을 수 없음")
    void findById_not_found() {
      when(userRepository.findById(1L)).thenReturn(Optional.empty());

      assertThrows(CustomException.class, () -> {
        userService.findById(1L);
      });
    }

    @Test
    @DisplayName("성공 - 이메일로 사용자 조회")
    void findByEmail_success() {
      User mockUser = mock(User.class);
      when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));

      Optional<User> result = userService.findByEmail("test@test.com");

      assertTrue(result.isPresent());
      assertEquals(mockUser, result.get());
    }

    @Test
    @DisplayName("성공 - 이메일 존재 확인")
    void existsByEmail_true() {
      when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

      boolean result = userService.existsByEmail("test@test.com");

      assertTrue(result);
    }

    @Test
    @DisplayName("성공 - 전화번호 존재 확인")
    void existsByPhoneNumber_true() {
      when(userRepository.existsByPhoneNumber("01012345678")).thenReturn(true);

      boolean result = userService.existsByPhoneNumber("01012345678");

      assertTrue(result);
    }
  }

  @Nested
  @DisplayName("사용자 정보 수정 테스트")
  class UpdateUserInfoTest {

    @Test
    @DisplayName("성공 - 사용자 정보 수정")
    void updateUserInfo_success() {
      User mockUser = mock(User.class);
      when(mockUser.getPhoneNumber()).thenReturn("01012345678");
      when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
      when(userRepository.existsByPhoneNumber("01087654321")).thenReturn(false);
      when(userRepository.save(mockUser)).thenReturn(mockUser);

      User result = userService.updateUserInfo(1L, "새이름", "01087654321");

      assertNotNull(result);
      verify(mockUser, times(1)).updateUserInfo("새이름", "01087654321");
      verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    @DisplayName("실패 - 전화번호 중복")
    void updateUserInfo_fail_duplicate_phone() {
      User mockUser = mock(User.class);
      when(mockUser.getPhoneNumber()).thenReturn("01012345678");
      when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
      when(userRepository.existsByPhoneNumber("01087654321")).thenReturn(true);

      assertThrows(CustomException.class, () -> {
        userService.updateUserInfo(1L, "새이름", "01087654321");
      });

      verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("성공 - 동일한 전화번호로 수정")
    void updateUserInfo_same_phone() {
      User mockUser = mock(User.class);
      when(mockUser.getPhoneNumber()).thenReturn("01012345678");
      when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
      when(userRepository.save(mockUser)).thenReturn(mockUser);

      User result = userService.updateUserInfo(1L, "새이름", "01012345678");

      assertNotNull(result);
      verify(userRepository, never()).existsByPhoneNumber(anyString());
      verify(userRepository, times(1)).save(mockUser);
    }
  }

  @Nested
  @DisplayName("비밀번호 변경 테스트")
  class ChangePasswordTest {

    @Test
    @DisplayName("성공 - 비밀번호 변경")
    void changePassword_success() {
      User mockUser = mock(User.class);
      when(mockUser.getPassword()).thenReturn("encodedOldPassword");
      when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
      when(passwordEncoder.matches("oldPassword", "encodedOldPassword")).thenReturn(true);
      when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");

      userService.changePassword(1L, "oldPassword", "newPassword");

      verify(mockUser, times(1)).updatePassword("encodedNewPassword");
      verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    @DisplayName("실패 - 현재 비밀번호 불일치")
    void changePassword_fail_wrong_current_password() {
      User mockUser = mock(User.class);
      when(mockUser.getPassword()).thenReturn("encodedOldPassword");
      when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
      when(passwordEncoder.matches("wrongPassword", "encodedOldPassword")).thenReturn(false);

      assertThrows(CustomException.class, () -> {
        userService.changePassword(1L, "wrongPassword", "newPassword");
      });

      verify(userRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("비밀번호 업데이트 테스트")
  class UpdatePasswordTest {

    @Test
    @DisplayName("성공 - 비밀번호 업데이트")
    void updatePassword_success() {
      User mockUser = mock(User.class);
      when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
      when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");

      userService.updatePassword(1L, "newPassword");

      verify(mockUser, times(1)).updatePassword("encodedNewPassword");
      verify(userRepository, times(1)).save(mockUser);
    }
  }

  @Nested
  @DisplayName("관리자 권한 관리 테스트")
  class AdminRoleTest {

    @Test
    @DisplayName("성공 - 관리자 권한 부여")
    void grantAdminRole_success() {
      User mockUser = mock(User.class);
      when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

      userService.grantAdminRole(1L);

      verify(mockUser, times(1)).grantAdminRole();
      verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    @DisplayName("성공 - 관리자 권한 제거")
    void revokeAdminRole_success() {
      User mockUser = mock(User.class);
      when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

      userService.revokeAdminRole(1L);

      verify(mockUser, times(1)).revokeAdminRole();
      verify(userRepository, times(1)).save(mockUser);
    }
  }
}