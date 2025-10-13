package com.deliveranything.domain.store.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deliveranything.domain.store.category.entity.StoreCategory;
import com.deliveranything.domain.store.category.service.StoreCategoryService;
import com.deliveranything.domain.store.store.dto.StoreCreateRequest;
import com.deliveranything.domain.store.store.dto.StoreResponse;
import com.deliveranything.domain.store.store.dto.StoreUpdateRequest;
import com.deliveranything.domain.store.store.entity.Store;
import com.deliveranything.domain.store.store.enums.StoreStatus;
import com.deliveranything.domain.store.store.event.StoreDeletedEvent;
import com.deliveranything.domain.store.store.event.StoreSavedEvent;
import com.deliveranything.domain.store.store.repository.StoreRepository;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreService 테스트")
class StoreServiceTest {

  @Mock
  private StoreRepository storeRepository;

  @Mock
  private StoreCategoryService storeCategoryService;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private StoreService storeService;

  private StoreCategory createTestStoreCategory(Long id, String name) {
    StoreCategory storeCategory = new StoreCategory(name);
    try {
      Field idField = storeCategory.getClass().getSuperclass().getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(storeCategory, id);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("Failed to set ID for StoreCategory", e);
    }
    return storeCategory;
  }

  // Helper method to create a Store object for testing
  private Store createTestStore(Long id, Long sellerProfileId, String name, StoreStatus status, StoreCategory storeCategory) {
    Store store = Store.builder()
        .sellerProfileId(sellerProfileId)
        .name(name)
        .storeCategory(storeCategory)
        .roadAddr("Test Road")
        .imageUrl("test.jpg")
        .location(null)
        .build();
    store.updateStatus(status);
    try {
      Field idField = store.getClass().getSuperclass().getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(store, id);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("Failed to set ID for Store", e);
    }
    return store;
  }

  @Test
  @DisplayName("상점 생성 성공")
  void createStore_success() {
    // given
    Long storeCategoryId = 1L;
    StoreCreateRequest request = new StoreCreateRequest(storeCategoryId, "Test Store", "Description", "Road Addr", 37.0, 127.0, "image.jpg");
    Long sellerProfileId = 1L;
    StoreCategory storeCategory = createTestStoreCategory(storeCategoryId, "Category");

    when(storeRepository.existsBySellerProfileId(sellerProfileId)).thenReturn(false);
    when(storeCategoryService.getById(storeCategoryId)).thenReturn(storeCategory);

    doAnswer(new Answer<Store>() {
      @Override
      public Store answer(InvocationOnMock invocation) throws Throwable {
        Store storeToSave = invocation.getArgument(0);
        try {
          Field idField = storeToSave.getClass().getSuperclass().getDeclaredField("id");
          idField.setAccessible(true);
          idField.set(storeToSave, 1L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
          throw new RuntimeException("Failed to set ID for Store", e);
        }
        return storeToSave;
      }
    }).when(storeRepository).save(any(Store.class));

    // when
    Long createdStoreId = storeService.createStore(request, sellerProfileId);

    // then
    assertThat(createdStoreId).isEqualTo(1L);
    verify(storeRepository).existsBySellerProfileId(sellerProfileId);
    verify(storeCategoryService).getById(storeCategoryId);
    verify(storeRepository).save(any(Store.class));
    verify(eventPublisher).publishEvent(any(StoreSavedEvent.class));
  }

  @Test
  @DisplayName("상점 생성 실패 - 이미 상점이 존재함")
  void createStore_fail_alreadyExists() {
    // given
    Long storeCategoryId = 1L;
    StoreCreateRequest request = new StoreCreateRequest(storeCategoryId, "Test Store", "Description", "Road Addr", 37.0, 127.0, "image.jpg");
    Long sellerProfileId = 1L;

    when(storeRepository.existsBySellerProfileId(sellerProfileId)).thenReturn(true);

    // when & then
    CustomException exception = assertThrows(CustomException.class, () ->
        storeService.createStore(request, sellerProfileId));
    assertThat(exception.getCode()).isEqualTo(ErrorCode.STORE_ALREADY_EXISTS.getCode());
    verify(storeRepository).existsBySellerProfileId(sellerProfileId);
    verify(storeCategoryService, never()).getById(anyLong());
    verify(storeRepository, never()).save(any(Store.class));
    verify(eventPublisher, never()).publishEvent(any(StoreSavedEvent.class));
  }

  @Test
  @DisplayName("상점 업데이트 성공")
  void updateStore_success() {
    // given
    Long storeId = 1L;
    Long originalStoreCategoryId = 1L;
    Long updatedStoreCategoryId = 2L;
    StoreUpdateRequest request = new StoreUpdateRequest(
        updatedStoreCategoryId,
        "Updated Store",
        "Updated Desc",
        "Updated Addr",
        38.0,
        128.0,
        "updated.jpg"
    );
    StoreCategory originalCategory = createTestStoreCategory(originalStoreCategoryId, "Original Category");
    Store existingStore = createTestStore(storeId, 1L, "Original Store", StoreStatus.CLOSED, originalCategory);
    StoreCategory updatedCategory = createTestStoreCategory(updatedStoreCategoryId, "Updated Category");

    when(storeRepository.getById(storeId)).thenReturn(existingStore);
    when(storeCategoryService.getById(updatedStoreCategoryId)).thenReturn(updatedCategory);

    // when
    StoreResponse response = storeService.updateStore(storeId, request);

    // then
    assertThat(response.name()).isEqualTo("Updated Store");
    assertThat(response.description()).isEqualTo("Updated Desc");
    assertThat(response.imageUrl()).isEqualTo("updated.jpg");
    assertThat(existingStore.getStoreCategory().getId()).isEqualTo(updatedStoreCategoryId);
    verify(storeRepository).getById(storeId);
    verify(storeCategoryService).getById(updatedStoreCategoryId);
    verify(eventPublisher).publishEvent(any(StoreSavedEvent.class));
  }

  @Test
  @DisplayName("상점 삭제 성공")
  void deleteStore_success() {
    // given
    Long storeId = 1L;
    StoreCategory storeCategory = createTestStoreCategory(1L, "Category");
    Store existingStore = createTestStore(storeId, 1L, "Test Store", StoreStatus.CLOSED, storeCategory);

    when(storeRepository.getById(storeId)).thenReturn(existingStore);
    doNothing().when(storeRepository).delete(any(Store.class));

    // when
    storeService.deleteStore(storeId);

    // then
    verify(storeRepository).getById(storeId);
    verify(storeRepository).delete(existingStore);
    verify(eventPublisher).publishEvent(any(StoreDeletedEvent.class));
  }

  @Test
  @DisplayName("상점 조회 성공")
  void getStore_success() {
    // given
    Long storeId = 1L;
    StoreCategory storeCategory = createTestStoreCategory(1L, "Category");
    Store existingStore = createTestStore(storeId, 1L, "Test Store", StoreStatus.OPEN, storeCategory);

    when(storeRepository.getById(storeId)).thenReturn(existingStore);

    // when
    StoreResponse response = storeService.getStore(storeId);

    // then
    assertThat(response.id()).isEqualTo(storeId);
    assertThat(response.name()).isEqualTo("Test Store");
    verify(storeRepository).getById(storeId);
  }

  @Test
  @DisplayName("상점 상태 토글 성공 - CLOSED -> OPEN")
  void toggleStoreStatus_closedToOpen_success() {
    // given
    Long storeId = 1L;
    StoreCategory storeCategory = createTestStoreCategory(1L, "Category");
    Store existingStore = createTestStore(storeId, 1L, "Test Store", StoreStatus.CLOSED, storeCategory);

    when(storeRepository.getById(storeId)).thenReturn(existingStore);

    // when
    StoreResponse response = storeService.toggleStoreStatus(storeId);

    // then
    assertThat(response.status()).isEqualTo(StoreStatus.OPEN);
    verify(storeRepository).getById(storeId);
    verify(eventPublisher).publishEvent(any(StoreSavedEvent.class));
  }

  @Test
  @DisplayName("상점 상태 토글 성공 - OPEN -> CLOSED")
  void toggleStoreStatus_openToClosed_success() {
    // given
    Long storeId = 1L;
    StoreCategory storeCategory = createTestStoreCategory(1L, "Category");
    Store existingStore = createTestStore(storeId, 1L, "Test Store", StoreStatus.OPEN, storeCategory);

    when(storeRepository.getById(storeId)).thenReturn(existingStore);

    // when
    StoreResponse response = storeService.toggleStoreStatus(storeId);

    // then
    assertThat(response.status()).isEqualTo(StoreStatus.CLOSED);
    verify(storeRepository).getById(storeId);
    verify(eventPublisher).publishEvent(any(StoreSavedEvent.class));
  }

  @Test
  @DisplayName("상점 상태 토글 실패 - DRAFT 상태")
  void toggleStoreStatus_fail_draftStatus() {
    // given
    Long storeId = 1L;
    StoreCategory storeCategory = createTestStoreCategory(1L, "Category");
    Store existingStore = createTestStore(storeId, 1L, "Test Store", StoreStatus.DRAFT, storeCategory);

    when(storeRepository.getById(storeId)).thenReturn(existingStore);

    // when & then
    CustomException exception = assertThrows(CustomException.class, () ->
        storeService.toggleStoreStatus(storeId));
    assertThat(exception.getCode()).isEqualTo(ErrorCode.STORE_NOT_READY_FOR_OPENING.getCode());
    verify(storeRepository).getById(storeId);
    verify(eventPublisher, never()).publishEvent(any(StoreSavedEvent.class));
  }

  @Test
  @DisplayName("SellerProfile ID로 Store ID 조회 성공 - 상점 존재")
  void getStoreIdBySellerProfileId_storeExists_success() {
    // given
    Long sellerProfileId = 1L;
    StoreCategory storeCategory = createTestStoreCategory(1L, "Category");
    Store store = createTestStore(1L, sellerProfileId, "Test Store", StoreStatus.OPEN, storeCategory);

    when(storeRepository.findBySellerProfileId(sellerProfileId)).thenReturn(Optional.of(store));

    // when
    Long result = storeService.getStoreIdBySellerProfileId(sellerProfileId);

    // then
    assertThat(result).isEqualTo(1L);
    verify(storeRepository).findBySellerProfileId(sellerProfileId);
  }

  @Test
  @DisplayName("SellerProfile ID로 Store ID 조회 성공 - 상점 없음")
  void getStoreIdBySellerProfileId_storeNotExists_success() {
    // given
    Long sellerProfileId = 1L;

    when(storeRepository.findBySellerProfileId(sellerProfileId)).thenReturn(Optional.empty());

    // when
    Long result = storeService.getStoreIdBySellerProfileId(sellerProfileId);

    // then
    assertThat(result).isNull();
    verify(storeRepository).findBySellerProfileId(sellerProfileId);
  }
}