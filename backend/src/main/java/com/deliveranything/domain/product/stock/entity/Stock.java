package com.deliveranything.domain.product.stock.entity;

import com.deliveranything.domain.product.product.entity.Product;
import com.deliveranything.global.entity.BaseEntity;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient; // Import for @Transient
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "stocks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock extends BaseEntity {

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Version
  private Integer version;

  @Column(name = "quantity", nullable = false, columnDefinition = "INT DEFAULT 0")
  private Integer totalQuantity;

  @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
  private Integer heldQuantity;

  @Transient
  public Integer getAvailableQuantity() {
    return this.totalQuantity - this.heldQuantity;
  }

  // 재고 예약
  public boolean reserve(int quantity) {
    if (getAvailableQuantity() < quantity) {
      return false;
    }
    this.heldQuantity += quantity;
    return true;
  }

  // 재고 차감 확정
  public boolean commit(int quantity) {
    if (this.heldQuantity < quantity) {
      return false;
    }
    this.heldQuantity -= quantity;
    this.totalQuantity -= quantity;
    return true;
  }

  // 재고 예약 해제
  public boolean release(int quantity) {
    if (this.heldQuantity < quantity) {
      return false;
    }
    this.heldQuantity -= quantity;
    return true;
  }

  // 재고 차감 복구
  public void replenish(int quantity) {
    this.totalQuantity += quantity;
  }

  // 관리자용 재고 세팅 totalQuantity 직접 변경
  public void setTotalQuantity(int newQuantity) {
    if (newQuantity < 0) throw new CustomException(ErrorCode.STOCK_CHANGE_INVALID);
    this.totalQuantity = newQuantity;
  }

  public Stock(Product product, Integer quantity) {
    this.product = product;
    this.totalQuantity = quantity;
    this.heldQuantity = 0;
    product.setStock(this);
  }
}