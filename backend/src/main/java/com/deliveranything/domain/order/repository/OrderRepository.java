package com.deliveranything.domain.order.repository;

import com.deliveranything.domain.order.entity.Order;
import com.deliveranything.domain.order.enums.OrderStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {

  Optional<Order> findByMerchantId(String merchantId);

  @Query("SELECT o FROM Order o JOIN FETCH o.store WHERE o.id = :orderId")
  Optional<Order> findOrderWithStoreById(Long orderId);

  @Query("SELECT o FROM Order o JOIN FETCH o.store WHERE o.id = :orderId AND o.customer.id = :customerId")
  Optional<Order> findOrderWithStoreByIdAndCustomerId(Long orderId, Long customerId);

  @Query("SELECT o FROM Order o JOIN FETCH o.store WHERE o.merchantId = :merchantId")
  Optional<Order> findOrderWithStoreByMerchantId(String merchantId);

  @Query("SELECT o FROM Order o JOIN FETCH o.store WHERE o.delivery.id = :deliveryId")
  Optional<Order> findOrderWithStoreByDeliveryId(Long deliveryId);

  @Query("SELECT o FROM Order o JOIN FETCH o.store WHERE o.store.id = :storeId AND o.status = :status")
  List<Order> findOrdersWithStoreByStoreIdAndStatus(Long storeId, OrderStatus status);

  @Query("SELECT o FROM Order o JOIN FETCH o.store WHERE o.delivery.riderProfile.id = :riderProfileId")
  List<Order> findOrdersWithStoreByRiderProfile(Long riderProfileId);

  @Query("""
      SELECT o
      FROM Order o
      JOIN FETCH o.store s
      WHERE s.id = :storeId AND o.status IN :statuses
      ORDER BY o.createdAt ASC
      """)
  List<Order> findOrdersWithStoreByStoreIdAndStatuses(Long storeId, List<OrderStatus> statuses);

  @Query("""
      SELECT o
      FROM Order o
      JOIN FETCH o.store s
      WHERE s.id = :customerId AND o.status IN :statuses
      ORDER BY o.createdAt DESC
      """)
  List<Order> findOrdersWithStoreByCustomerIdAndStatuses(Long customerId,
      List<OrderStatus> statuses);
}
