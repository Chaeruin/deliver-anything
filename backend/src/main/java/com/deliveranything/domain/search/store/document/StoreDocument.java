package com.deliveranything.domain.search.store.document;

import com.deliveranything.domain.store.store.entity.Store;
import com.deliveranything.domain.store.store.enums.StoreStatus;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

@Getter
@Builder
@Document(indexName = "stores")
public class StoreDocument {

  @Id
  private Long id;

  @Field(type = FieldType.Text, name = "name", analyzer = "nori")
  private String name;

  @Field(type = FieldType.Text, name = "description", analyzer = "nori")
  private String description;

  // 검색 필터링을 위한 카테고리 ID
  @Field(type = FieldType.Long, name = "category_id")
  private Long categoryId;

  @Field(type = FieldType.Keyword, name = "store_category")
  private String categoryName;

  @Field(type = FieldType.Text, name = "image_url")
  private String imageUrl;

  @Field(type = FieldType.Text, name = "road_address")
  private String roadAddress;

  @GeoPointField
  private GeoPoint location;

  @Field(type = FieldType.Keyword, name = "status")
  private StoreStatus status;

  @Field(type = FieldType.Date, name = "created_at", format = DateFormat.date_time)
  private OffsetDateTime createdAt;

  @Setter
  @Builder.Default
  @Field(type = FieldType.Text, name = "keywords", analyzer = "nori")
  private List<String> keywords = new ArrayList<>();

  public static StoreDocument from(Store store) {
    return StoreDocument.builder()
        .id(store.getId())
        .name(store.getName())
        .description(store.getDescription())
        .categoryName(store.getStoreCategory().getName())
        .location(new GeoPoint(store.getLocation().getY(), store.getLocation().getX()))
        .status(store.getStatus())
        .categoryId(store.getStoreCategory().getId())
        .roadAddress(store.getRoadAddr())
        .imageUrl(store.getImageUrl())
        .createdAt(store.getCreatedAt().atOffset(ZoneOffset.ofHours(9)))
        .build();
  }
}