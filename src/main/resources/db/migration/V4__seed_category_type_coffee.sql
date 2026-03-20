-- Seed default category type.
-- Other types can be added later via admin UI or next migrations.
INSERT INTO category_type (code, name, description, sort_order)
VALUES ('coffee', 'Кофе', 'Базовый тип категории для кофейных напитков', 10)
ON CONFLICT (code) DO NOTHING;

