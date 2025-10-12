package com.deliveranything.domain.search.store.repository;

import co.elastic.clients.elasticsearch._types.DistanceUnit;
import co.elastic.clients.elasticsearch._types.GeoDistanceType;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.deliveranything.domain.search.store.document.StoreDocument;
import com.deliveranything.domain.search.store.dto.StoreSearchRequest;
import com.deliveranything.global.common.CursorPageResponse;
import com.deliveranything.global.util.CursorUtil;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class StoreSearchRepositoryImpl implements StoreSearchRepositoryCustom {

  private final ElasticsearchOperations elasticsearchOperations;

  private static final String FIELD_NAME = "name";
  private static final String FIELD_DESCRIPTION = "description";
  private static final String FIELD_KEYWORDS = "keywords";
  private static final String FIELD_CATEGORY_ID = "category_id";
  private static final String FIELD_LOCATION = "location";
  private static final String SORT_ID = "id";

  @Override
  public CursorPageResponse<StoreDocument> search(StoreSearchRequest request) {
    int querySize = request.limit() + 1;

    // 검색 조건 구성
    BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
    applyFilters(boolQueryBuilder, request);

    NativeQueryBuilder queryBuilder = new NativeQueryBuilder()
        .withQuery(Query.of(q -> q.bool(boolQueryBuilder.build())))
        .withPageable(PageRequest.of(0, querySize));

    // 정렬 조건
    if (request.lat() != null && request.lng() != null) {
      queryBuilder
          .withSort(s -> s.geoDistance(g -> g
              .field(FIELD_LOCATION)
              .location(l -> l.latlon(ll -> ll.lat(request.lat()).lon(request.lng())))
              .unit(DistanceUnit.Kilometers)
              .distanceType(GeoDistanceType.Arc)
              .order(SortOrder.Asc)));
    }

    // _id 정렬 또한 알 수 없는 이유로 실패하여, 가장 안정적인 _doc 순서로 정렬;;
    queryBuilder.withSort(s -> s.field(f -> f.field("_doc").order(SortOrder.Asc)));

    // 커서(search_after) 처리
    if (StringUtils.hasText(request.nextPageToken())) {
      Object[] decodedCursor = CursorUtil.decode(request.nextPageToken());
      if (decodedCursor != null) {
        Object[] safeCursor = Arrays.stream(decodedCursor)
            .map(value -> {
              if (value instanceof Number) return value;
              if (value instanceof String s) return s;
              return value;
            })
            .toArray();
        queryBuilder.withSearchAfter(Arrays.asList(safeCursor));
      }
    }

    NativeQuery searchQuery = queryBuilder.build();

    // 검색 실행
    SearchHits<StoreDocument> searchHits;
    try {
      searchHits = elasticsearchOperations.search(searchQuery, StoreDocument.class);
    } catch (Exception e) {
      throw new RuntimeException("Failed to search store documents. Check logs for mapping or query errors.", e);
    }

    List<SearchHit<StoreDocument>> hits = searchHits.getSearchHits();
    List<StoreDocument> documents = hits.stream()
        .map(SearchHit::getContent)
        .collect(Collectors.toList());

    // 다음 페이지 여부 확인
    boolean hasNext = documents.size() > request.limit();
    List<StoreDocument> responseDocuments = hasNext
        ? documents.subList(0, request.limit())
        : documents;

    // 다음 페이지 토큰 생성
    String nextToken = null;
    if (hasNext) {
      SearchHit<StoreDocument> lastHit = hits.get(request.limit() - 1);
      Object[] lastSortValues = lastHit.getSortValues().stream()
          .map(value -> {
            if (value instanceof Number) return value;
            if (value instanceof String s) return s;
            return value;
          })
          .toArray();
      nextToken = CursorUtil.encode(lastSortValues);
    }

    return new CursorPageResponse<>(responseDocuments, nextToken, hasNext);
  }

  // 검색 필터 구성
  private void applyFilters(BoolQuery.Builder boolQueryBuilder, StoreSearchRequest request) {
    if (StringUtils.hasText(request.searchText())) {
      boolQueryBuilder.must(m -> m.multiMatch(mm -> mm
          .query(request.searchText())
          .fields(FIELD_NAME, FIELD_DESCRIPTION, FIELD_KEYWORDS)
      ));
    }

    if (request.categoryId() != null) {
      boolQueryBuilder.filter(f -> f.term(t -> t
          .field(FIELD_CATEGORY_ID)
          .value(request.categoryId())));
    }

    if (request.lat() != null && request.lng() != null) {
      if (request.distanceKm() != null) {
        boolQueryBuilder.filter(f -> f.geoDistance(g -> g
            .field(FIELD_LOCATION)
            .location(l -> l.latlon(ll -> ll.lat(request.lat()).lon(request.lng())))
            .distance(request.distanceKm() + "km")
            .distanceType(GeoDistanceType.Arc)
        ));
      } else {
        // 거리순 정렬 시 location 필드가 없는 도큐먼트는 오류를 유발할 수 있으므로, 필드가 존재하는 도큐먼트만 필터링
        boolQueryBuilder.filter(f -> f.exists(e -> e.field(FIELD_LOCATION)));
      }
    }
  }
}