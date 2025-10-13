package com.deliveranything.domain.search.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deliveranything.domain.product.product.entity.Product;
import com.deliveranything.domain.product.product.repository.ProductRepository;
import com.deliveranything.domain.search.store.document.StoreDocument;
import com.deliveranything.domain.search.store.repository.StoreSearchRepository;
import com.deliveranything.domain.store.category.entity.StoreCategory;
import com.deliveranything.domain.store.store.entity.Store;
import com.deliveranything.global.util.PointUtil;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StoreKeywordSyncServiceTest {

  @Mock
  private ProductRepository productRepository;

  @Mock
  private StoreSearchRepository storeSearchRepository;

  @InjectMocks
  private StoreKeywordSyncService storeKeywordSyncService;

  private Long storeId;
  private StoreDocument storeDocument;
  private Store store;

  @BeforeEach
  void setUp() {
    storeId = 1L;
    StoreCategory storeCategory = new StoreCategory("Category");
    ReflectionTestUtils.setField(storeCategory, "id", 1L);
    store = Store.builder()
        .name("Test Store")
        .sellerProfileId(1L)
        .storeCategory(storeCategory)
        .imageUrl("url")
        .description("desc")
        .roadAddr("addr")
        .location(PointUtil.createPoint(0,0))
        .build();

    ReflectionTestUtils.setField(store, "id", storeId);

    storeDocument = StoreDocument.builder()
        .id(storeId)
        .name("Test Store")
        .build();
  }

  @Test
  @DisplayName("키워드 동기화 성공 테스트 - 제품 키워드 존재")
  void syncKeywordsSuccessWithProductKeywordsTest() {
    Product product1 = Product.builder().store(store).name("Product A").price(10000).imageUrl("url1").initialStock(10).build();
    ReflectionTestUtils.setField(product1, "id", 1L);
    product1.setKeywords("keyword1,keyword2");

    Product product2 = Product.builder()
        .store(store)
        .name("Product B")
        .price(20000)
        .imageUrl("url2")
        .initialStock(20)
        .build();

    ReflectionTestUtils.setField(product2, "id", 2L);
    product2.setKeywords("keyword2,keyword3");

    when(storeSearchRepository.findById(storeId)).thenReturn(Optional.of(storeDocument));
    when(productRepository.findAllByStoreId(storeId)).thenReturn(List.of(product1, product2));

    storeKeywordSyncService.syncKeywords(storeId);

    verify(storeSearchRepository, times(1)).findById(storeId);
    verify(productRepository, times(1)).findAllByStoreId(storeId);
    verify(storeSearchRepository, times(1)).save(storeDocument);

    assertThat(storeDocument.getKeywords()).containsExactlyInAnyOrder("keyword1", "keyword2", "keyword3");
  }

  @Test
  @DisplayName("키워드 동기화 성공 테스트 - 제품 키워드 없음")
  void syncKeywordsSuccessNoProductKeywordsTest() {
    when(storeSearchRepository.findById(storeId)).thenReturn(Optional.of(storeDocument));
    when(productRepository.findAllByStoreId(storeId)).thenReturn(List.of());

    storeKeywordSyncService.syncKeywords(storeId);

    verify(storeSearchRepository, times(1)).findById(storeId);
    verify(productRepository, times(1)).findAllByStoreId(storeId);
    verify(storeSearchRepository, times(1)).save(storeDocument);

    assertThat(storeDocument.getKeywords()).isEmpty();
  }

  @Test
  @DisplayName("키워드 동기화 테스트 - StoreDocument를 찾을 수 없음")
  void syncKeywordsStoreDocumentNotFoundTest() {
    when(storeSearchRepository.findById(storeId)).thenReturn(Optional.empty());

    storeKeywordSyncService.syncKeywords(storeId);

    verify(storeSearchRepository, times(1)).findById(storeId);
    verify(productRepository, never()).findAllByStoreId(storeId);
    verify(storeSearchRepository, never()).save(storeDocument);

    assertThat(storeDocument.getKeywords()).isEmpty();
  }
}