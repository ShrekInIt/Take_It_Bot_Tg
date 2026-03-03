package com.example.bot.Telegram_bot_take_it.repository;

import com.example.bot.Telegram_bot_take_it.admin.dto.OrderDto;
import com.example.bot.Telegram_bot_take_it.entity.Order;
import com.example.bot.Telegram_bot_take_it.entity.OrderItem;
import com.example.bot.Telegram_bot_take_it.entity.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
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

    @NotNull
    @Query("SELECT o FROM Order o JOIN FETCH o.user ORDER BY o.createdAt DESC")
    List<Order> findAll();

    long countByStatusIn(Collection<Order.OrderStatus> status);

    @Query("""
    SELECT o
    FROM Order o
    join fetch o.user u
    WHERE LOWER(o.user.name) LIKE CONCAT('%', :username, '%')
    order by o.createdAt DESC
""")
    List<Order> findByUsername(String username);

    @Query("""
    select new com.example.bot.Telegram_bot_take_it.admin.dto.OrderDto(
        o.id,
        o.status,
        o.totalAmount,
        o.user.name,
        o.user.phoneNumber,
        o.deliveryType,
        o.createdAt,
        o.comments
    )
    from Order o
    where o.id = :id
""")
    Optional<OrderDto> findByIdWithDetails(@Param("id") Long id);

    /**
     * Загружаем заказ вместе с items, product, addons, user — чтобы не столкнуться с LazyInitializationException.
     */
    @NotNull
    @EntityGraph(attributePaths = {
            "items",
            "items.product",
            "items.addons",
            "user"
    })
    Optional<Order> findById(@NotNull Long id);

    @Query("select o.user from Order o where o.id = :orderId")
    Optional<User> findUserIdByOrderId(@Param("orderId") Long orderId);


    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.createdAt BETWEEN :start AND :end AND  o.status = 'COMPLETED' ")
    Long sumTotalAmountByDateOrderBetween(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);


    @EntityGraph(attributePaths = {"user"})
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Integer countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
