package com.deliveranything.domain.product.stock.service;

import com.deliveranything.domain.product.stock.entity.Stock;
import com.deliveranything.domain.product.stock.repository.StockRepository;
import com.deliveranything.domain.store.store.entity.Store;
import com.deliveranything.domain.store.store.repository.StoreRepository;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockCommandService {

  private final StockRepository stockRepository;
  private final StoreRepository storeRepository;

  @Transactional
  public Stock getStockForUpdate(Long storeId, Long productId) {
    Stock stock = stockRepository.getByProductId(productId);
    stock.getProduct().validateStore(storeId);
    return stock;
  }

  @Transactional(readOnly = true)
  public void checkStoreOpen(Long storeId) {
    Store store = storeRepository.findById(storeId)
        .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

    if (!store.isOpen()) {
      throw new CustomException(ErrorCode.STORE_CLOSED);
    }
  }

  public void holdStock(Stock stock, int quantity) {
    if (!stock.reserve(quantity)) throw new CustomException(ErrorCode.PRODUCT_OUT_OF_STOCK);
  }

  public void commitStock(Stock stock, int quantity) {
    if (!stock.commit(quantity)) throw new CustomException(ErrorCode.STOCK_CHANGE_INVALID);
  }

  public void releaseStock(Stock stock, int quantity) {
    if (!stock.release(quantity)) throw new CustomException(ErrorCode.STOCK_CHANGE_INVALID);
  }

  public void replenishStock(Stock stock, int quantity) {
    stock.replenish(quantity);
  }
}