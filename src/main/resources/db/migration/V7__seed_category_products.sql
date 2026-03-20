INSERT INTO category (name, description, parent_id, is_active, sort_order, category_type_id)
VALUES
    ('Латте', 'Кофейные напитки: латте', NULL, true, 10, NULL),
    ('Раф', 'Классические и авторские рафы', NULL, true, 20, NULL);

-- Раф авторский как дочерняя категория Раф
INSERT INTO category (name, description, parent_id, is_active, sort_order, category_type_id)
SELECT
    'Раф авторский',
    'Авторские вкусы рафа',
    c.id,
    true,
    21,
    NULL
FROM category c
WHERE c.name = 'Раф'
  AND NOT EXISTS (
    SELECT 1 FROM category WHERE name = 'Раф авторский'
);

INSERT INTO product (name, amount, size, count, available, photo, description, category_id)
SELECT
    'Латте',
    220,
    '300мл',
    0,
    true,
    NULL,
    'Классический латте',
    c.id
FROM category c
WHERE c.name = 'Латте'
  AND NOT EXISTS (
    SELECT 1 FROM product p
    WHERE p.name = 'Латте'
      AND p.size = '300мл'
);

INSERT INTO product (name, amount, size, count, available, photo, description, category_id)
SELECT
    t.name,
    t.amount,
    t.size,
    0 AS count,
    true AS available,
    NULL AS photo,
    t.description,
    t.category_id
FROM (
                  SELECT 'Раф классический' AS name, 210 AS amount, '200мл' AS size, 'Классический раф' AS description, c.id AS category_id
                  FROM category c
                  WHERE c.name = 'Раф'

                  UNION ALL

                  SELECT 'Раф классический', 310, '400мл', 'Классический раф', c.id
                  FROM category c
                  WHERE c.name = 'Раф'

                  UNION ALL

                  SELECT 'Раф хвойный', 240, '200мл', 'Раф хвойный', c.id
                  FROM category c
                  WHERE c.name = 'Раф'

                  UNION ALL

                  SELECT 'Раф хвойный', 350, '400мл', 'Раф хвойный', c.id
                  FROM category c
                  WHERE c.name = 'Раф'

                  UNION ALL

                  SELECT 'Раф тропический', 240, '200мл', 'Раф тропический', c.id
                  FROM category c
                  WHERE c.name = 'Раф'

                  UNION ALL

                  SELECT 'Раф тропический', 350, '400мл', 'Раф тропический', c.id
                  FROM category c
                  WHERE c.name = 'Раф'

                  UNION ALL

                  SELECT 'Раф вишнёвая кола', 240, '200мл', 'Раф со вкусом вишнёвой колы', c.id
                  FROM category c
                  WHERE c.name = 'Раф'

                  UNION ALL

                  SELECT 'Раф вишнёвая кола', 350, '400мл', 'Раф со вкусом вишнёвой колы', c.id
                  FROM category c
                  WHERE c.name = 'Раф'

                  UNION ALL

                  SELECT 'Раф малина/мята', 240, '200мл', 'Раф малина/мята', c.id
                  FROM category c
                  WHERE c.name = 'Раф'

                  UNION ALL

                  SELECT 'Раф малина/мята', 350, '400мл', 'Раф малина/мята', c.id
                  FROM category c
                  WHERE c.name = 'Раф'

                  UNION ALL

                  SELECT 'Раф яблоко/ревень', 240, '200мл', 'Раф яблоко/ревень', c.id
                  FROM category c
                  WHERE c.name = 'Раф'

                  UNION ALL

                  SELECT 'Раф яблоко/ревень', 350, '400мл', 'Раф яблоко/ревень', c.id
                  FROM category c
                  WHERE c.name = 'Раф'
              ) t
WHERE NOT EXISTS (
    SELECT 1
    FROM product p
    WHERE p.name = t.name
      AND p.size = t.size
);

INSERT INTO product (name, amount, size, count, available, photo, description, category_id)
SELECT
    t.name,
    t.amount,
    t.size,
    0 AS count,
    true AS available,
    NULL AS photo,
    t.description,
    t.category_id
FROM (
                  SELECT 'Раф карамель' AS name, 260 AS amount, '200мл' AS size, 'Авторский раф: карамель' AS description, c.id AS category_id
                  FROM category c
                  WHERE c.name = 'Раф авторский'

                  UNION ALL

                  SELECT 'Раф карамель', 370, '400мл', 'Авторский раф: карамель', c.id
                  FROM category c
                  WHERE c.name = 'Раф авторский'

                  UNION ALL

                  SELECT 'Раф арахис', 260, '200мл', 'Авторский раф: арахис', c.id
                  FROM category c
                  WHERE c.name = 'Раф авторский'

                  UNION ALL

                  SELECT 'Раф арахис', 370, '400мл', 'Авторский раф: арахис', c.id
                  FROM category c
                  WHERE c.name = 'Раф авторский'

                  UNION ALL

                  SELECT 'Раф лаванда', 260, '200мл', 'Авторский раф: лаванда', c.id
                  FROM category c
                  WHERE c.name = 'Раф авторский'

                  UNION ALL

                  SELECT 'Раф лаванда', 370, '400мл', 'Авторский раф: лаванда', c.id
                  FROM category c
                  WHERE c.name = 'Раф авторский'

                  UNION ALL

                  SELECT 'Раф цитрус', 260, '200мл', 'Авторский раф: цитрус', c.id
                  FROM category c
                  WHERE c.name = 'Раф авторский'

                  UNION ALL

                  SELECT 'Раф цитрус', 370, '400мл', 'Авторский раф: цитрус', c.id
                  FROM category c
                  WHERE c.name = 'Раф авторский'

                  UNION ALL

                  SELECT 'Раф халва', 260, '200мл', 'Авторский раф: халва', c.id
                  FROM category c
                  WHERE c.name = 'Раф авторский'

                  UNION ALL

                  SELECT 'Раф халва', 370, '400мл', 'Авторский раф: халва', c.id
                  FROM category c
                  WHERE c.name = 'Раф авторский'
              ) t
WHERE NOT EXISTS (
    SELECT 1
    FROM product p
    WHERE p.name = t.name
      AND p.size = t.size
);