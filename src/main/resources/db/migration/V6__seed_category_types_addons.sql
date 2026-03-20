-- Seed additional category types.
-- Existing records are preserved.
INSERT INTO category_type (code, name, description, sort_order)
VALUES
    ('addons', 'Добавки', 'Тип категории для добавок', 50)
ON CONFLICT (code) DO NOTHING;

