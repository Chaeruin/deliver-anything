package com.deliveranything.domain.order.entity;

import com.deliveranything.domain.delivery.entity.Delivery;
import com.deliveranything.domain.order.enums.OrderStatus;
import com.deliveranything.domain.store.store.entity.Store;
import com.deliveranything.domain.user.profile.entity.CustomerProfile;
import com.deliveranything.global.entity.BaseEntity;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;

@Slf4j
@Getter
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "store_id", nullable = false)
  private Store store;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id", nullable = false)
  private CustomerProfile customer;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "delivery_id", unique = true)
  private Delivery delivery;

  @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
  private List<OrderItem> orderItems = new ArrayList<>();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus status;

  @Column(nullable = false, unique = true, length = 200)
  private String merchantId;

  @Column(nullable = false, length = 100)
  private String address;

  @Column(columnDefinition = "POINT SRID 4326", nullable = false)
  private Point destination;

  @Column(length = 30)
  private String riderNote;

  @Column(length = 30)
  private String storeNote;

  @Column(nullable = false)
  private Long totalPrice;

  @Column(nullable = false)
  private Long storePrice;

  @Column(nullable = false)
  private Long deliveryPrice;

  private String cancellationReason;

  @Builder
  public Order(CustomerProfile customer, Store store, String address, Point destination,
      String riderNote, String storeNote, Long totalPrice, Long storePrice, Long deliveryPrice) {
    this.customer = customer;
    this.store = store;
    this.address = address;
    this.destination = destination;
    this.riderNote = riderNote;
    this.storeNote = storeNote;
    this.totalPrice = totalPrice;
    this.storePrice = storePrice;
    this.deliveryPrice = deliveryPrice;
    this.status = OrderStatus.PENDING;
    this.merchantId = UUID.randomUUID().toString();
  }

  public void addOrderItem(OrderItem orderItem) {
    this.orderItems.add(orderItem);
    orderItem.setOrder(this);
  }

  public void updateStatus(OrderStatus status) {
    if (!this.status.canTransitTo(status)) {
      log.warn("order status can't be transited at {} to {} ", this.status, status);
    }

    this.status = status;
  }

  public void isPayable() {
    if (this.status != OrderStatus.CREATED) {
      throw new CustomException(ErrorCode.ORDER_PAY_UNAVAILABLE);
    }
  }

  public void isCancelable() {
    if (this.status != OrderStatus.PENDING) {
      throw new CustomException(ErrorCode.ORDER_CANCEL_UNAVAILABLE);
    }
  }

  public void cancel(String reason) {
    updateStatus(OrderStatus.CANCELED);
    this.cancellationReason = reason;
  }

  public void cancellationRequest(String reason) {
    isCancelable();
    updateStatus(OrderStatus.CANCELLATION_REQUESTED);
    this.cancellationReason = reason;
  }
}
