package com.deliveranything.domain.product.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deliveranything.domain.product.product.dto.ProductCreateRequest;
import com.deliveranything.domain.product.product.dto.ProductDetailResponse;
import com.deliveranything.domain.product.product.dto.ProductResponse;
import com.deliveranything.domain.product.product.dto.ProductSearchRequest;
import com.deliveranything.domain.product.product.dto.ProductUpdateRequest;
import com.deliveranything.domain.product.product.entity.Product;
import com.deliveranything.domain.product.product.repository.ProductRepository;
import com.deliveranything.domain.store.store.entity.Store;
import com.deliveranything.domain.store.store.service.StoreService;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService 테스트")
class ProductServiceTest {

  @Mock
  private ProductRepository productRepository;

  @Mock
  private StoreService storeService;

  @Mock
  private KeywordGenerationService keywordGenerationService;

  @InjectMocks
  private ProductService productService;

  // Helper method to create a Store object with ID for testing
  private Store createTestStore(Long id, Long sellerProfileId, String name) {
    Store store = Store.builder()
        .sellerProfileId(sellerProfileId)
        .name(name)
        .storeCategory(null) // Not relevant for product tests
        .roadAddr("Test Road")
        .imageUrl("test.jpg")
        .location(null)
        .build();
    try {
      Field idField = store.getClass().getSuperclass().getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(store, id);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("Failed to set ID for Store", e);
    }
    return store;
  }

  // Helper method to create a Product object with ID for testing
  private Product createTestProduct(Long id, Store store, String name, String description, Integer price, String imageUrl, Integer initialStock) {
    Product product = Product.builder()
        .store(store)
        .name(name)
        .description(description)
        .price(price)
        .imageUrl(imageUrl)
        .initialStock(initialStock)
        .build();
    try {
      Field idField = product.getClass().getSuperclass().getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(product, id);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("Failed to set ID for Product", e);
    }
    return product;
  }

  @Test
  @DisplayName("상품 생성 성공")
  void createProduct_success() {
    // given
    Long storeId = 1L;
    ProductCreateRequest request = new ProductCreateRequest("Test Product", "Description", 1000, "product.jpg", 10);
    Store store = createTestStore(storeId, 1L, "Test Store");

    Product savedProductMock = mock(Product.class);
    when(savedProductMock.getId()).thenReturn(1L);
    when(savedProductMock.getName()).thenReturn(request.name());
    when(savedProductMock.getStore()).thenReturn(store); // Mock the store for ProductResponse.from

    when(storeService.getStoreById(storeId)).thenReturn(store);
    when(productRepository.save(any(Product.class))).thenReturn(savedProductMock);
    doNothing().when(keywordGenerationService).generateAndSaveKeywords(anyLong());

    // when
    ProductResponse response = productService.createProduct(storeId, request);

    // then
    assertThat(response.productId()).isEqualTo(1L);
    assertThat(response.name()).isEqualTo("Test Product");
    verify(storeService).getStoreById(storeId);
    verify(productRepository).save(any(Product.class));
    verify(keywordGenerationService).generateAndSaveKeywords(1L);
  }

  @Test
  @DisplayName("상품 삭제 성공")
  void deleteProduct_success() {
    // given
    Long storeId = 1L;
    Long productId = 1L;
    Store store = createTestStore(storeId, 1L, "Test Store");
    Product product = mock(Product.class); // Make product a mock
    // when(product.getId()).thenReturn(productId); // Not directly used by deleteProduct or validateStore

    when(productRepository.getById(productId)).thenReturn(product);
    doNothing().when(productRepository).delete(any(Product.class));

    // when
    productService.deleteProduct(storeId, productId);

    // then
    verify(productRepository).getById(productId);
    verify(product).validateStore(storeId); // Verify interaction with the mock product
    verify(productRepository).delete(product);
  }

  @Test
  @DisplayName("상품 삭제 실패 - 상점 ID 불일치")
  void deleteProduct_fail_storeIdMismatch() {
    // given
    Long storeId = 1L;
    Long productId = 1L;
    Long wrongStoreId = 2L;
    Store store = createTestStore(storeId, 1L, "Test Store");
    Product product = mock(Product.class); // Make product a mock
    // when(product.getId()).thenReturn(productId); // Not directly used by deleteProduct or validateStore

    when(productRepository.getById(productId)).thenReturn(product);
    doThrow(new CustomException(ErrorCode.PRODUCT_STORE_MISMATCH))
        .when(product).validateStore(wrongStoreId);

    // when & then
    CustomException exception = assertThrows(CustomException.class, () ->
        productService.deleteProduct(wrongStoreId, productId));
    assertThat(exception.getCode()).isEqualTo(ErrorCode.PRODUCT_STORE_MISMATCH.getCode());
    verify(productRepository).getById(productId);
    verify(productRepository, never()).delete(any(Product.class));
    verify(product).validateStore(wrongStoreId); // Verify interaction with the mock product
  }

  @Test
  @DisplayName("상품 업데이트 성공")
  void updateProduct_success() {
    // given
    Long storeId = 1L;
    Long productId = 1L;
    ProductUpdateRequest request = new ProductUpdateRequest("Updated Product", "Updated Desc", 2000, 20, "updated.jpg");
    Store store = createTestStore(storeId, 1L, "Test Store");
    Product existingProduct = createTestProduct(productId, store, "Original Product", "Original Desc", 1000, "original.jpg", 10);

    when(productRepository.getById(productId)).thenReturn(existingProduct);
    doNothing().when(keywordGenerationService).generateAndSaveKeywords(anyLong());

    // when
    ProductResponse response = productService.updateProduct(storeId, productId, request);

    // then
    assertThat(response.name()).isEqualTo("Updated Product");
    assertThat(response.price()).isEqualTo(2000);
    assertThat(response.imageUrl()).isEqualTo("updated.jpg");
    assertThat(existingProduct.getStock().getTotalQuantity()).isEqualTo(20);
    verify(productRepository).getById(productId);
    verify(keywordGenerationService).generateAndSaveKeywords(productId);
  }

  @Test
  @DisplayName("상품 조회 성공")
  void getProduct_success() {
    // given
    Long storeId = 1L;
    Long productId = 1L;
    Store store = createTestStore(storeId, 1L, "Test Store");
    Product product = createTestProduct(productId, store, "Test Product", "Description", 1000, "product.jpg", 10);

    when(productRepository.getById(productId)).thenReturn(product);

    // when
    ProductDetailResponse response = productService.getProduct(storeId, productId);

    // then
    assertThat(response.productId()).isEqualTo(productId);
    assertThat(response.name()).isEqualTo("Test Product");
    verify(productRepository).getById(productId);
  }

  @Test
  @DisplayName("상품 검색 성공")
  void searchProducts_success() {
    // given
    Long storeId = 1L;
    ProductSearchRequest request = new ProductSearchRequest("keyword", null, 10);
    Store store = createTestStore(storeId, 1L, "Test Store");
    Product product = createTestProduct(1L, store, "Found Product", "Description", 1000, "product.jpg", 10);
    SliceImpl<Product> productSlice = new SliceImpl<>(List.of(product), PageRequest.of(0, 10), false);

    when(storeService.getStoreById(storeId)).thenReturn(store);
    when(productRepository.search(storeId, request)).thenReturn(productSlice);

    // when
    var responseSlice = productService.searchProducts(storeId, request);

    // then
    assertThat(responseSlice.getContent()).hasSize(1);
    assertThat(responseSlice.getContent().get(0).name()).isEqualTo("Found Product");
    verify(storeService).getStoreById(storeId);
    verify(productRepository).search(storeId, request);
  }

  @Test
  @DisplayName("ID로 상품 조회 성공")
  void getProductById_success() {
    // given
    Long productId = 1L;
    Store store = createTestStore(1L, 1L, "Test Store");
    Product product = createTestProduct(productId, store, "Test Product", "Description", 1000, "product.jpg", 10);

    when(productRepository.getById(productId)).thenReturn(product);

    // when
    Product foundProduct = productService.getProductById(productId);

    // then
    assertThat(foundProduct.getId()).isEqualTo(productId);
    assertThat(foundProduct.getName()).isEqualTo("Test Product");
    verify(productRepository).getById(productId);
  }
}