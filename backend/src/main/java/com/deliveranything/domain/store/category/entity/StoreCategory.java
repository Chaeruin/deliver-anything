package com.deliveranything.domain.store.category.entity;

import com.deliveranything.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "store_category")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreCategory extends BaseEntity {

  @Column(nullable = false)
  private String name;

  public StoreCategory(String name) {
    this.name = name;
  }

//  @OneToMany(mappedBy = "storeCategory")
//  private List<Store> stores = new ArrayList<>();
}
