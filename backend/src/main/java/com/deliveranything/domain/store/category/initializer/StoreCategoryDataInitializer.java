package com.deliveranything.domain.store.category.initializer;

import com.deliveranything.domain.store.category.entity.StoreCategory;
import com.deliveranything.domain.store.category.repository.StoreCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StoreCategoryDataInitializer implements CommandLineRunner {

    private final StoreCategoryRepository storeCategoryRepository;

    @Override
    public void run(String... args) {
        List<String> categoryNames = Arrays.asList(
            "음식/카페", "편의점/마트", "생활/잡화", "뷰티/헬스", "전자/IT",
            "패션/의류", "스포츠/레저", "서점/문화", "반려동물용품", "꽃/원예",
            "자동차", "공구/철물"
        );

        List<StoreCategory> existingCategories = storeCategoryRepository.findAll();
        List<String> existingCategoryNames = existingCategories.stream()
            .map(StoreCategory::getName)
            .toList();

        List<StoreCategory> categoriesToSave = categoryNames.stream()
            .filter(name -> !existingCategoryNames.contains(name))
            .map(StoreCategory::new)
            .collect(Collectors.toList());

        if (!categoriesToSave.isEmpty()) {
            storeCategoryRepository.saveAll(categoriesToSave);
        }
    }
}
