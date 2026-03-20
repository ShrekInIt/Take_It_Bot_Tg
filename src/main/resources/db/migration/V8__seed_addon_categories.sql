
INSERT INTO category (id, name, description, parent_id, is_active, sort_order, category_type_id)
VALUES
    (20, 'Сиропы', 'Добавки: сиропы', NULL, TRUE, 10, (SELECT id FROM category_type WHERE code = 'addons')),
    (21, 'Альтернативное молоко', 'Добавки: альт. молоко', NULL, TRUE, 20, (SELECT id FROM category_type WHERE code = 'addons'))
    ON CONFLICT (id) DO UPDATE
                            SET
                                name = EXCLUDED.name,
                            description = EXCLUDED.description,
                            is_active = EXCLUDED.is_active,
                            sort_order = EXCLUDED.sort_order,
                            category_type_id = EXCLUDED.category_type_id;

SELECT setval(pg_get_serial_sequence('category', 'id'), (SELECT MAX(id) FROM category));

INSERT INTO product (name, amount, size, count, available, photo, description, category_id)
SELECT 'Сироп ванильный', 35, NULL, 100, TRUE, NULL, 'Добавка к кофе', 20
    WHERE NOT EXISTS (
    SELECT 1 FROM product WHERE name = 'Сироп ванильный' AND category_id = 20
);

INSERT INTO product (name, amount, size, count, available, photo, description, category_id)
SELECT 'Молоко кокосовое', 50, NULL, 100, TRUE, NULL, 'Добавка к кофе', 21
    WHERE NOT EXISTS (
    SELECT 1 FROM product WHERE name = 'Молоко кокосовое' AND category_id = 21
);
