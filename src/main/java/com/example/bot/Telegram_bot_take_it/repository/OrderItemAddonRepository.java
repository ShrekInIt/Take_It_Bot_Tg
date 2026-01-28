package com.example.bot.Telegram_bot_take_it.repository;

import com.example.bot.Telegram_bot_take_it.admin.dto.OrderItemAddonDto;
import com.example.bot.Telegram_bot_take_it.entity.OrderItemAddon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemAddonRepository extends JpaRepository<OrderItemAddon, Long> {
    @Query("""
    select new com.example.bot.Telegram_bot_take_it.admin.dto.OrderItemAddonDto(
        a.id,
        a.addonProductName,
        a.quantity,
        a.priceAtOrder
    )
    from OrderItemAddon a
    where a.orderItem.id = :orderItemId
""")
    List<OrderItemAddonDto> findByOrderItemId(@Param("orderItemId") Long orderItemId);
}
