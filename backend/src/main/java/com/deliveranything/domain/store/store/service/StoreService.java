package com.deliveranything.domain.store.store.service;

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
import com.deliveranything.global.util.PointUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

  private final StoreRepository storeRepository;
  private final StoreCategoryService storeCategoryService;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public Long createStore(StoreCreateRequest request, Long sellerProfileId) {
    if (storeRepository.existsBySellerProfileId(sellerProfileId)) {
      throw new CustomException(ErrorCode.STORE_ALREADY_EXISTS);
    }

    StoreCategory storeCategory = storeCategoryService.getById(request.storeCategoryId());

    Store store = Store.builder()
        .sellerProfileId(sellerProfileId)
        .storeCategory(storeCategory)
        .name(request.name())
        .description(request.description())
        .roadAddr(request.roadAddr())
        .location(PointUtil.createPoint(request.lat(), request.lng()))
        .imageUrl(request.imageUrl())
        .build();
    store.updateStatus(StoreStatus.CLOSED);

    storeRepository.save(store);

    eventPublisher.publishEvent(new StoreSavedEvent(store.getId()));

    return store.getId();
  }

  @Transactional
  public StoreResponse updateStore(Long storeId, StoreUpdateRequest request) {
    Store store = this.getStoreById(storeId);

    StoreCategory storeCategory = null;
    if (request.storeCategoryId() != null) {
      storeCategory = storeCategoryService.getById(request.storeCategoryId());
    }

    store.update(storeCategory, request.name(), request.description(), request.roadAddr(),
        request.lat() != null && request.lng() != null ? PointUtil.createPoint(request.lat(),
            request.lng()) : null, request.imageUrl());

    eventPublisher.publishEvent(new StoreSavedEvent(store.getId()));

    return StoreResponse.from(store);
  }

  @Transactional
  public void deleteStore(Long storeId) {
    Store store = this.getStoreById(storeId);
    storeRepository.delete(store);
    eventPublisher.publishEvent(new StoreDeletedEvent(store.getId()));
  }

  @Transactional(readOnly = true)
  public StoreResponse getStore(Long storeId) {
    Store store = this.getStoreById(storeId);
    return StoreResponse.from(store);
  }

  @Transactional
  public StoreResponse toggleStoreStatus(Long storeId) {
    Store store = this.getStoreById(storeId);

    if (store.getStatus() == StoreStatus.DRAFT) {
      throw new CustomException(ErrorCode.STORE_NOT_READY_FOR_OPENING);
    }

    StoreStatus newStatus =
        (store.getStatus() == StoreStatus.OPEN) ? StoreStatus.CLOSED : StoreStatus.OPEN;
    store.updateStatus(newStatus);

    eventPublisher.publishEvent(new StoreSavedEvent(store.getId()));
    return StoreResponse.from(store);
  }

  @Transactional(readOnly = true)
  public Store getStoreById(Long storeId) {
    return storeRepository.getById(storeId);
  }

  /**
   * author: darancode - SellerProfile ID로 Store ID 조회 Store가 없으면 null 반환
   */
  public Long getStoreIdBySellerProfileId(Long sellerProfileId) {
    return storeRepository.findBySellerProfileId(sellerProfileId)
        .map(Store::getId)
        .orElse(null);
  }

}