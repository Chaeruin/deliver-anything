package com.deliveranything.domain.search.store.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deliveranything.domain.search.store.document.StoreDocument;
import com.deliveranything.domain.search.store.repository.StoreSearchRepository;
import com.deliveranything.domain.store.category.entity.StoreCategory;
import com.deliveranything.domain.store.store.entity.Store;
import com.deliveranything.domain.store.store.service.StoreService;
import com.deliveranything.global.util.PointUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StoreSyncServiceTest {

  @Mock
  private StoreService storeService;

  @Mock
  private StoreSearchRepository storeSearchRepository;

  @InjectMocks
  private StoreSyncService storeSyncService;

  private Long storeId;

  @BeforeEach
  void setUp() {
    storeId = 1L;
  }

  @Test
  @DisplayName("상점 저장 이벤트 처리 테스트")
  void handleSavedTest() {
    Store store = Store.builder()
        .name("Test Store")
        .sellerProfileId(1L)
        .storeCategory(new StoreCategory("Category"))
        .imageUrl("url")
        .description("desc")
        .roadAddr("addr")
        .location(PointUtil.createPoint(0,0))
        .build();

    ReflectionTestUtils.setField(store, "id", storeId);
    when(storeService.getStoreById(storeId)).thenReturn(store);

    try (MockedStatic<StoreDocument> mockedStoreDocument = Mockito.mockStatic(StoreDocument.class)) {
      StoreDocument storeDocument = mock(StoreDocument.class);
      mockedStoreDocument.when(() -> StoreDocument.from(store)).thenReturn(storeDocument);

      storeSyncService.handleSaved(storeId);

      verify(storeService, times(1)).getStoreById(storeId);
      verify(storeSearchRepository, times(1)).save(storeDocument);
    }
  }

  @Test
  @DisplayName("상점 삭제 이벤트 처리 테스트")
  void handleDeletedTest() {
    storeSyncService.handleDeleted(storeId);

    verify(storeSearchRepository, times(1)).deleteById(storeId);
  }
}