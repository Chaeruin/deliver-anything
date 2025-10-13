package com.deliveranything.domain.product.stock.service;

import com.deliveranything.domain.product.stock.dto.StockResponse;
import com.deliveranything.domain.product.stock.repository.StockRepository;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockQueryService {

  private final StockRepository stockRepository;

  @Transactional(readOnly = true)
  public StockResponse getProductStock(Long storeId, Long productId) {
    var stock = stockRepository.findByProductId(productId)
        .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
    stock.getProduct().validateStore(storeId);
    return StockResponse.from(stock);
  }
}