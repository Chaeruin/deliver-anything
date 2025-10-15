package com.deliveranything.domain.product.stock.service;

import com.deliveranything.domain.order.event.dto.OrderItemInfo;
import com.deliveranything.domain.product.stock.entity.Stock;
import com.deliveranything.domain.product.stock.event.StockCommittedEvent;
import com.deliveranything.domain.product.stock.event.StockReleasedEvent;
import com.deliveranything.domain.product.stock.event.StockReplenishedEvent;
import com.deliveranything.domain.product.stock.event.StockReserveFailedEvent;
import com.deliveranything.domain.product.stock.event.StockReservedEvent;
import jakarta.persistence.OptimisticLockException;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        if (retries >= MAX_RETRIES) {
          throw e;
        }
      }
    }
  }

  @Transactional
  public void handleOrderCreated(Long orderId, Long storeId, List<OrderItemInfo> items) {
    try {
      stockCommandService.checkStoreOpen(storeId);
      executeWithRetry(() -> {
        for (var item : items) {
          Stock stock = stockCommandService.getStockForUpdate(storeId, item.productId());
          stockCommandService.holdStock(stock, item.quantity());
        }
        return null;
      });
      eventPublisher.publishEvent(new StockReservedEvent(orderId));
    } catch (Exception e) {
      log.error("Failed to reserve stock for order, orderId={}, storeId={}", orderId, storeId, e);
      eventPublisher.publishEvent(new StockReserveFailedEvent(orderId, e.getMessage()));
      throw e;
    }
  }

  @Transactional
  public void handleOrderCancelSucceeded(Long orderId, Long storeId, List<OrderItemInfo> items) {
    executeWithRetry(() -> {
      for (var item : items) {
        Stock stock = stockCommandService.getStockForUpdate(storeId, item.productId());
        stockCommandService.replenishStock(stock, item.quantity());
      }
      return null;
    });
    eventPublisher.publishEvent(new StockReplenishedEvent(orderId));
  }

  @Transactional
  public void handleOrderPaymentSucceeded(Long orderId, Long storeId, List<OrderItemInfo> items) {
    stockCommandService.checkStoreOpen(storeId);
    executeWithRetry(() -> {
      for (var item : items) {
        Stock stock = stockCommandService.getStockForUpdate(storeId, item.productId());
        stockCommandService.commitStock(stock, item.quantity());
      }
      return null;
    });
    eventPublisher.publishEvent(new StockCommittedEvent(orderId));
  }

  @Transactional
  public void handleOrderPaymentFailed(Long orderId, Long storeId, List<OrderItemInfo> items) {
    executeWithRetry(() -> {
      for (var item : items) {
        Stock stock = stockCommandService.getStockForUpdate(storeId, item.productId());
        stockCommandService.releaseStock(stock, item.quantity());
      }
      return null;
    });
    eventPublisher.publishEvent(new StockReleasedEvent(orderId));
  }
}