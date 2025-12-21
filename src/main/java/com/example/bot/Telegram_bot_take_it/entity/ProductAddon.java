package com.example.bot.Telegram_bot_take_it.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_addon",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "addon_product_id"}))
@Data
public class ProductAddon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addon_product_id", nullable = false)
    private Product addonProduct;

    @Column(name = "additional_price")
    private Integer additionalPrice = 0;

    @Column(name = "price_for_small_volume")
    private Integer priceForSmallVolume;

    @Column(name = "price_for_large_volume")
    private Integer priceForLargeVolume;

    @Column(name = "small_volume_threshold")
    private Integer smallVolumeThreshold = 200;

    @Column(name = "volume_dependent")
    private Boolean volumeDependent = false;

    @Column(name = "is_required")
    private Boolean isRequired = false;

    @Column(name = "max_quantity")
    private Integer maxQuantity = 1;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
