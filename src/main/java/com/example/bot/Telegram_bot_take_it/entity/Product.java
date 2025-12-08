package com.example.bot.Telegram_bot_take_it.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "product")
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "amount")
    private Integer amount;

    @Column(name = "size")
    private String size;

    @Column(name = "count")
    private Integer count = 0;

    @Column(name = "available")
    private Boolean available = true;

    @Column(name = "photo")
    private String photo;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category_id")
    private Long categoryId;

    @ManyToOne
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;

    // Связь с добавками (когда этот продукт является основным)
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductAddon> addons;

    // Связь с добавками (когда этот продукт является добавкой)
    @OneToMany(mappedBy = "addonProduct", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductAddon> usedAsAddon;

}
