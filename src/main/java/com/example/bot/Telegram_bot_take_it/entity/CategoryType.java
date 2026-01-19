package com.example.bot.Telegram_bot_take_it.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "category_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", unique = true, nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
