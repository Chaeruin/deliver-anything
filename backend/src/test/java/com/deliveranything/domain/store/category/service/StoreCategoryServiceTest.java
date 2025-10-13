package com.deliveranything.domain.store.category.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deliveranything.domain.store.category.dto.StoreCategoryResponse;
import com.deliveranything.domain.store.category.entity.StoreCategory;
import com.deliveranything.domain.store.category.repository.StoreCategoryRepository;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreCategoryService 테스트")
class StoreCategoryServiceTest {

  @Mock
  private StoreCategoryRepository storeCategoryRepository;

  @InjectMocks
  private StoreCategoryService storeCategoryService;

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

  @Test
  @DisplayName("모든 카테고리 조회 성공")
  void getAllCategories_success() {
    // given
    StoreCategory category1 = createTestStoreCategory(1L, "Category A");
    StoreCategory category2 = createTestStoreCategory(2L, "Category B");
    when(storeCategoryRepository.findAll()).thenReturn(List.of(category1, category2));

    // when
    List<StoreCategoryResponse> responses = storeCategoryService.getAllCategories();

    // then
    assertThat(responses).hasSize(2);
    assertThat(responses.get(0).id()).isEqualTo(1L);
    assertThat(responses.get(0).name()).isEqualTo("Category A");
    assertThat(responses.get(1).id()).isEqualTo(2L);
    assertThat(responses.get(1).name()).isEqualTo("Category B");
    verify(storeCategoryRepository).findAll();
  }

  @Test
  @DisplayName("ID로 카테고리 조회 성공")
  void getById_success() {
    // given
    Long categoryId = 1L;
    StoreCategory category = createTestStoreCategory(categoryId, "Category A");
    when(storeCategoryRepository.getById(categoryId)).thenReturn(category);

    // when
    StoreCategory foundCategory = storeCategoryService.getById(categoryId);

    // then
    assertThat(foundCategory.getId()).isEqualTo(categoryId);
    assertThat(foundCategory.getName()).isEqualTo("Category A");
    verify(storeCategoryRepository).getById(categoryId);
  }
}