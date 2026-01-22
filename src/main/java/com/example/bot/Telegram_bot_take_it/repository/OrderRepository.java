package com.example.bot.Telegram_bot_take_it.repository;

import com.example.bot.Telegram_bot_take_it.entity.Order;
import com.example.bot.Telegram_bot_take_it.entity.OrderItem;
import com.example.bot.Telegram_bot_take_it.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.items i " +
            "LEFT JOIN FETCH i.product " +
            "WHERE o.user = :user " +
            "AND o.visible = true " +
            "ORDER BY o.createdAt DESC")
    List<Order> findByUserWithItems(@Param("user") User user);

    @Query("SELECT DISTINCT i FROM OrderItem i " +
            "LEFT JOIN FETCH i.addons a " +
            "LEFT JOIN FETCH i.product " +
            "LEFT JOIN FETCH a.addonProduct " +
            "WHERE i.order.id = :orderId")
    List<OrderItem> findOrderItemsWithAddons(@Param("orderId") Long orderId);

    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.items i " +
            "LEFT JOIN FETCH i.product " +
            "WHERE o.id = :orderId AND o.user = :user")
    Optional<Order> findByIdAndUserWithItems(@Param("orderId") Long orderId, @Param("user") User user);

    List<Order> findByUserId(Long userId);

    long countByStatusIn(List<String> statuses);

    @Query("SELECT o FROM Order o JOIN FETCH o.user ORDER BY o.createdAt DESC LIMIT 10")
    List<Order> findTop10ByOrderByDateOrderDesc();

    @Query("SELECT o FROM Order o JOIN FETCH o.user")
    List<Order> findAllWithUser();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
            "WHERE o.createdAt >= :startDate AND o.createdAt < :endDate " +
            "AND o.status = :status")
    Long sumTotalAmountByDateAndStatus(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate,
                                       @Param("status") String status);
}
