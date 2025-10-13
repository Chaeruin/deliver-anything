package com.deliveranything.domain.store.store.entity;

import com.deliveranything.domain.store.category.entity.StoreCategory;
import com.deliveranything.domain.store.store.enums.StoreStatus;
import com.deliveranything.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

@Entity
@Table(
    name = "stores",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_stores_seller", columnNames = "seller_profile_id")
    },
    indexes = {
        @Index(name = "idx_stores_category", columnList = "store_category_id"),
        @Index(name = "idx_stores_status", columnList = "status"),
        @Index(name = "idx_stores_location", columnList = "location")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store extends BaseEntity {

  @Column(name = "seller_profile_id", nullable = false)
  private Long sellerProfileId;

  @ManyToOne
  @JoinColumn(name = "store_category_id", nullable = false)
  private StoreCategory storeCategory;

  @Column(name = "image_url", length = 255)
  private String imageUrl;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "road_addr", length = 255, nullable = false)
  private String roadAddr;

  @Column(columnDefinition = "geometry", nullable = false)
  private Point location;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 12)
  private StoreStatus status = StoreStatus.DRAFT;

  public void updateStatus(StoreStatus storeStatus) {
    status = storeStatus;
  }

  public boolean isOpen() { return status == StoreStatus.OPEN; }

  @Builder
  public Store(Long sellerProfileId, StoreCategory storeCategory, String imageUrl, String name, String description, String roadAddr, Point location) {
    this.sellerProfileId = sellerProfileId;
    this.storeCategory = storeCategory;
    this.imageUrl = imageUrl;
    this.name = name;
    this.description = description;
    this.roadAddr = roadAddr;
    this.location = location;
  }

  public void update(StoreCategory storeCategory, String name, String description, String roadAddr, Point location, String imageUrl) {
    if (storeCategory != null) {
      this.storeCategory = storeCategory;
    }
    if (name != null) {
      this.name = name;
    }
    if (description != null) {
      this.description = description;
    }
    if (roadAddr != null) {
      this.roadAddr = roadAddr;
    }
    if (location != null) {
      this.location = location;
    }
    if (imageUrl != null) {
      this.imageUrl = imageUrl;
    }
  }
}