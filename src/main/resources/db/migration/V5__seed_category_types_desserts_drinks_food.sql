INSERT INTO category_type (code, name, description, sort_order)
VALUES
    ('dessert', 'Десерты', 'Тип категории для десертов', 20),
    ('drink', 'Напитки', 'Тип категории для напитков', 30),
    ('food', 'Еда', 'Тип категории для еды', 40)
ON CONFLICT (code) DO NOTHING;

