package com.deliveranything.domain.product.stock.service;

import com.deliveranything.domain.order.event.dto.OrderItemInfo;
import com.deliveranything.domain.product.stock.entity.Stock;
import com.deliveranything.domain.product.stock.event.*;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockFacadeService {

  private static final int MAX_RETRIES = 3;

  private final StockCommandService stockCommandService;
  private final ApplicationEventPublisher eventPublisher;

  private <T> void executeWithRetry(Supplier<T> action) {
    int retries = 0;
    while (true) {
      try {
        action.get();
        return;
      } catch (OptimisticLockException e) {
        retries++;
        log.warn("Optimistic lock conflict, retry {}/{}", retries, MAX_RETRIES);
        if (retries >= MAX_RETRIES) throw e;
      }
    }
  }

  @Transactional
  public void handleOrderCreated(Long orderId, Long storeId, List<OrderItemInfo> items) {
    try {
      executeWithRetry(() -> {
        for (var item : items) {
          Stock stock = stockCommandService.getStockForUpdate(storeId, item.productId());
          stockCommandService.checkStoreOpen(storeId);
          stockCommandService.holdStock(stock, item.quantity().intValue());
        }
        return null;
      });
      eventPublisher.publishEvent(new StockReservedEvent(orderId, storeId, items));
    } catch (Exception e) {
      log.error("Failed to reserve stock for order, orderId={}, storeId={}", orderId, storeId, e);
      eventPublisher.publishEvent(new StockReservedFailedEvent(orderId, storeId, items, e.getMessage()));
      throw e;
    }
  }

  @Transactional
  public void handleOrderCancelSucceeded(Long orderId, Long storeId, List<OrderItemInfo> items) {
    executeWithRetry(() -> {
      for (var item : items) {
        Stock stock = stockCommandService.getStockForUpdate(storeId, item.productId());
        stockCommandService.replenishStock(stock, item.quantity().intValue());
      }
      return null;
    });
    eventPublisher.publishEvent(new StockReplenishedEvent(orderId, storeId, items));
  }

  @Transactional
  public void handleOrderPaymentSucceeded(Long orderId, Long storeId, List<OrderItemInfo> items) {
    executeWithRetry(() -> {
      for (var item : items) {
        Stock stock = stockCommandService.getStockForUpdate(storeId, item.productId());
        stockCommandService.checkStoreOpen(storeId);
        stockCommandService.commitStock(stock, item.quantity().intValue());
      }
      return null;
    });
    eventPublisher.publishEvent(new StockCommittedEvent(orderId, storeId, items));
  }

  @Transactional
  public void handleOrderPaymentFailed(Long orderId, Long storeId, List<OrderItemInfo> items) {
    executeWithRetry(() -> {
      for (var item : items) {
        Stock stock = stockCommandService.getStockForUpdate(storeId, item.productId());
        stockCommandService.releaseStock(stock, item.quantity().intValue());
      }
      return null;
    });
    eventPublisher.publishEvent(new StockReleasedEvent(orderId, storeId, items));
  }
}